/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <unistd.h>
#include <codecvt>
#include <sysexits.h>

#include <base/logging.h>
#include <base/command_line.h>
#include <base/macros.h>
#include <base/bind.h>
#include <binderwrapper/binder_wrapper.h>
#include <brillo/binder_watcher.h>
#include <brillo/daemons/daemon.h>
#include <brillo/syslog_logging.h>
#include <libweaved/service.h>

#include <mraa.h>

namespace {
	const char kWeaveComponent[] = "mydevice";
	const char kOnOffTrait[] = "onOff";
}

class DeviceDaemon final : public brillo::Daemon {
public:
	static int pinOnBoardLed;
	DeviceDaemon() = default;
protected:
	int OnInit() override;
	void OnWeaveServiceConnected(const std::weak_ptr<weaved::Service>& service);
	void OnPairingInfoChanged(const weaved::Service::PairingInfo* pairing_info);
	void UpdateDeviceState();
	// Command handlers
	void OnSetConfig(std::unique_ptr<weaved::Command> command);
private:
	/* the bridge between libbinder and brillo::MessageLoop */
	brillo::BinderWatcher binder_watcher_;
	/* the object subscribed to weaved */
	std::unique_ptr<weaved::Service::Subscription> weave_service_subscription_;
	/* the service instance */
	std::weak_ptr<weaved::Service> weave_service_;

	mraa_gpio_context ctxOnBoardLed;

	base::WeakPtrFactory<DeviceDaemon> weak_ptr_factory_{this};
	DISALLOW_COPY_AND_ASSIGN(DeviceDaemon);
};

int DeviceDaemon::pinOnBoardLed = 37;	/* MRAA number for GPIO-40 */

int DeviceDaemon::OnInit()
{
	int rc;
	if ((rc = brillo::Daemon::OnInit()) != EX_OK)
		return rc;

	/* Create and initialize the singleton for communicating with the real binder system */
	android::BinderWrapper::Create();
	if (!binder_watcher_.Init())
		return EX_OSERR;

	/* Create an instance of weaved service and establish an RPC connection to weaved daemon */
	weave_service_subscription_ = weaved::Service::Connect(
		brillo::MessageLoop::current(),
		base::Bind(&DeviceDaemon::OnWeaveServiceConnected, weak_ptr_factory_.GetWeakPtr()));

	ctxOnBoardLed = mraa_gpio_init(pinOnBoardLed);
	mraa_gpio_dir(ctxOnBoardLed, MRAA_GPIO_OUT);

	return EX_OK;
}

void DeviceDaemon::OnWeaveServiceConnected(const std::weak_ptr<weaved::Service>& service)
{
	LOG(INFO) << "DeviceDaemon::OnWeaveServiceConnected";
	/* upon connection the service instance is passed to the callback */
	weave_service_ = service;
	auto weave_service = weave_service_.lock();
	if (!weave_service)
		return;

	weave_service->AddComponent(::kWeaveComponent, { ::kOnOffTrait }, nullptr);

	weave_service->AddCommandHandler(
		::kWeaveComponent, ::kOnOffTrait, "setConfig",
		base::Bind(&DeviceDaemon::OnSetConfig, weak_ptr_factory_.GetWeakPtr()));

	weave_service->SetPairingInfoListener(
		base::Bind(&DeviceDaemon::OnPairingInfoChanged, weak_ptr_factory_.GetWeakPtr()));

	/* since a new instance will be passed to the callback when connection
	   is re-established, it's recommended to update the device state on each
       callback invocation */
	UpdateDeviceState();
}

void DeviceDaemon::OnPairingInfoChanged(const weaved::Service::PairingInfo* pairing_info)
{
	LOG(INFO) << "DeviceDaemon::OnPairingInfoChanged: " << pairing_info;
}

void DeviceDaemon::UpdateDeviceState()
{
	LOG(INFO) << "DeviceDaemon::UpdateDeviceState";
	std::string output_string = mraa_gpio_read(ctxOnBoardLed)? "on" : "off";
	auto weave_service = weave_service_.lock();
	if (!weave_service)
		return;
	base::DictionaryValue state_change;
	state_change.SetString("onOff.state", output_string);
	weave_service->SetStateProperties(::kWeaveComponent, state_change, nullptr);
}

void DeviceDaemon::OnSetConfig(std::unique_ptr<weaved::Command> command)
{
	std::string state = command->GetParameter<std::string>("state");
	LOG(INFO) << "Received command to set the device state to " << state;
	mraa_gpio_write(ctxOnBoardLed, !state.compare("on"));
	command->Complete({}, nullptr);
	UpdateDeviceState();
}

class Board {
public:
	Board() { mraa_init(); }
} board;

int main(int argc, char* argv[])
{
	base::CommandLine::Init(argc, argv);
	brillo::InitLog(brillo::kLogToSyslog | brillo::kLogHeader);
	DeviceDaemon daemon;
	return daemon.Run();
}

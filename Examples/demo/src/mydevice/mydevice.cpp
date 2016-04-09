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

#include "brillo/demo/BnOnOffService.h"
#include "on-off-service.h"
using brillo::demo::IOnOffService;

#include "brillo/demo/BnMp3PlayerService.h"
#include "mp3-player-service.h"
using brillo::demo::IMp3PlayerService;

namespace {
	const char Welcome[] = "     Brillo Jukebox demo running on Minnowboard";
	const char kWeaveComponent[] = "mydevice";
}

class DeviceDaemon final : public brillo::Daemon {
public:
	DeviceDaemon() = default;
protected:
	int OnInit() override;
	void OnWeaveServiceConnected(const std::weak_ptr<weaved::Service>& service);
	void OnPairingInfoChanged(const weaved::Service::PairingInfo* pairing_info);
	void OnOnOffServiceDisconnected();
	void OnMp3PlayerServiceDisconnected();

	void UpdateDeviceState();
	void UpdateOnOffTraitState();
	void UpdateMediaPlayerTraitState();

	void ConnectToOnOffService();
	void ConnectToMp3PlayerService();
	void TrackMp3PlayerReachedEOS();
	// Command handlers
	void OnSetConfig(std::unique_ptr<weaved::Command> command);
	void OnMp3Play(std::unique_ptr<weaved::Command> command);
	void OnMp3Pause(std::unique_ptr<weaved::Command> command);
	void OnMp3Stop(std::unique_ptr<weaved::Command> command);

	void SetDisplay(std::string msg);
private:
	/* the bridge between libbinder and brillo::MessageLoop */
	brillo::BinderWatcher binder_watcher_;
	/* the object subscribed to weaved */
	std::unique_ptr<weaved::Service::Subscription> weave_service_subscription_;
	/* the service instance */
	std::weak_ptr<weaved::Service> weave_service_;

	/* the On/Off service interface */
	android::sp<IOnOffService> on_off_service_;
	/* the MP3 player service interface */
	android::sp<IMp3PlayerService> mp3_player_service_;
	std::string mp3_current_playing;

	base::WeakPtrFactory<DeviceDaemon> weak_ptr_factory_{this};
	DISALLOW_COPY_AND_ASSIGN(DeviceDaemon);
};

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

	ConnectToOnOffService();
	ConnectToMp3PlayerService();

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

	weave_service->AddComponent(::kWeaveComponent, { on_off_service::kWeaveTrait,
	                                                 mp3_player_service::kWeaveTrait }, nullptr);
	weave_service->AddCommandHandler(
		::kWeaveComponent, on_off_service::kWeaveTrait, "setConfig",
		base::Bind(&DeviceDaemon::OnSetConfig, weak_ptr_factory_.GetWeakPtr()));
	weave_service->AddCommandHandler(
		::kWeaveComponent, mp3_player_service::kWeaveTrait, "play",
		base::Bind(&DeviceDaemon::OnMp3Play, weak_ptr_factory_.GetWeakPtr()));
	weave_service->AddCommandHandler(
		::kWeaveComponent, mp3_player_service::kWeaveTrait, "pause",
		base::Bind(&DeviceDaemon::OnMp3Pause, weak_ptr_factory_.GetWeakPtr()));
	weave_service->AddCommandHandler(
		::kWeaveComponent, mp3_player_service::kWeaveTrait, "stop",
		base::Bind(&DeviceDaemon::OnMp3Stop, weak_ptr_factory_.GetWeakPtr()));

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
	UpdateOnOffTraitState();
	UpdateMediaPlayerTraitState();
}

void DeviceDaemon::ConnectToOnOffService()
{
	android::BinderWrapper* binder_wrapper = android::BinderWrapper::Get();
	auto binder = binder_wrapper->GetService(on_off_service::kBinderServiceName);
	if (!binder.get()) {
		brillo::MessageLoop::current()->PostDelayedTask(
			base::Bind(&DeviceDaemon::ConnectToOnOffService, weak_ptr_factory_.GetWeakPtr()),
			base::TimeDelta::FromMilliseconds(500));
		return;
	}
	LOG(INFO) << "DeviceDaemon::OnOffServiceConnected";
	binder_wrapper->RegisterForDeathNotifications(binder,
		base::Bind(&DeviceDaemon::OnOnOffServiceDisconnected, weak_ptr_factory_.GetWeakPtr()));
	on_off_service_ = android::interface_cast<IOnOffService>(binder);
	SetDisplay(::Welcome);
	UpdateOnOffTraitState();
}

void DeviceDaemon::OnOnOffServiceDisconnected()
{
	LOG(INFO) << "DeviceDaemon::OnOnOffServiceDisconnected";
	on_off_service_ = nullptr;
	ConnectToOnOffService();
}

void DeviceDaemon::UpdateOnOffTraitState()
{
	LOG(INFO) << "DeviceDaemon::UpdateOnOffTraitState";
	if (on_off_service_.get()) {
		bool flag = false;
		android::binder::Status status = on_off_service_->getState(&flag);
		std::string output_string = "off";
		if (status.isOk() && flag)
			output_string = "on";

		auto weave_service = weave_service_.lock();
		if (!weave_service)
			return;

		base::DictionaryValue state_change;
		state_change.SetString("onOff.state", output_string);
		weave_service->SetStateProperties(::kWeaveComponent, state_change, nullptr);
	}
}

void DeviceDaemon::OnSetConfig(std::unique_ptr<weaved::Command> command)
{
	std::string state = command->GetParameter<std::string>("state");
	LOG(INFO) << "Received command to set the device state to " << state;

	if (!on_off_service_.get()) {
		command->Abort("_system_error", "On/Off service unavailable", nullptr);
		return;
	}
	bool flag = !state.compare("on");
	android::binder::Status status = on_off_service_->setState(flag);
	if (!status.isOk()) {
		command->AbortWithCustomError(status, nullptr);
		return;
	}
	command->Complete({}, nullptr);

	UpdateOnOffTraitState();
}

void DeviceDaemon::SetDisplay(std::string msg)
{
	if (on_off_service_.get()) {
		on_off_service_->setDisplay(::android::String16(msg.c_str()));
	}
}

void DeviceDaemon::ConnectToMp3PlayerService()
{
	android::BinderWrapper* binder_wrapper = android::BinderWrapper::Get();
	auto binder = binder_wrapper->GetService(mp3_player_service::kBinderServiceName);
	if (!binder.get()) {
		brillo::MessageLoop::current()->PostDelayedTask(
			base::Bind(&DeviceDaemon::ConnectToMp3PlayerService, weak_ptr_factory_.GetWeakPtr()),
			base::TimeDelta::FromMilliseconds(500));
		return;
	}
	LOG(INFO) << "DeviceDaemon::Mp3PlayerServiceConnected";
	binder_wrapper->RegisterForDeathNotifications(binder,
		base::Bind(&DeviceDaemon::OnMp3PlayerServiceDisconnected, weak_ptr_factory_.GetWeakPtr()));
	mp3_player_service_ = android::interface_cast<IMp3PlayerService>(binder);
	TrackMp3PlayerReachedEOS();
	UpdateMediaPlayerTraitState();
}

void DeviceDaemon::TrackMp3PlayerReachedEOS()
{
	if (mp3_player_service_.get()) {
		bool eos = true;
		android::binder::Status status = mp3_player_service_->reachedEOS(&eos);
		if (status.isOk() && mp3_current_playing.compare("-") != 0 && eos) {
			LOG(INFO) << "Advance to next track due to end of stream";
			status = mp3_player_service_->stop();
			if (status.isOk())
				status = mp3_player_service_->play();
			UpdateMediaPlayerTraitState();
		}
	}
	brillo::MessageLoop::current()->PostDelayedTask(
		base::Bind(&DeviceDaemon::TrackMp3PlayerReachedEOS, weak_ptr_factory_.GetWeakPtr()),
		base::TimeDelta::FromSeconds(3));
}

void DeviceDaemon::OnMp3PlayerServiceDisconnected()
{
	LOG(INFO) << "DeviceDaemon::OnMp3PlayerServiceDisconnected";
	mp3_player_service_ = nullptr;
	ConnectToMp3PlayerService();
}

void DeviceDaemon::UpdateMediaPlayerTraitState()
{
	LOG(INFO) << "DeviceDaemon::UpdateMediaPlayerTraitState";
	if (mp3_player_service_.get()) {
		::android::String16 player_info;
		android::binder::Status status = mp3_player_service_->status(&player_info);
		std::string player_state("unknown");
		if (status.isOk()) {
			std::wstring_convert<std::codecvt_utf8_utf16<char16_t>,char16_t> convert;
			player_state = convert.to_bytes(player_info.string());
			if (player_state.compare("idle") == 0) {
				SetDisplay(::Welcome);
				mp3_current_playing = "-";
			} else if (player_state.compare("paused") != 0) {
				mp3_current_playing = player_state;
				SetDisplay("     Playing: " + mp3_current_playing);
				player_state = "playing";
			}
		}

		auto weave_service = weave_service_.lock();
		if (!weave_service)
			return;

		base::DictionaryValue state_change;
		state_change.SetString("_mediaplayer.status", player_state);
		state_change.SetString("_mediaplayer.display", mp3_current_playing);
		weave_service->SetStateProperties(::kWeaveComponent, state_change, nullptr);
	}
}

void DeviceDaemon::OnMp3Play(std::unique_ptr<weaved::Command> command)
{
	LOG(INFO) << "Start MP3 playing...";
	if (!mp3_player_service_.get()) {
		command->Abort("_system_error", "MP3 player service unavailable", nullptr);
		return;
	}
	android::binder::Status status = mp3_player_service_->play();
	if (!status.isOk()) {
		command->AbortWithCustomError(status, nullptr);
		return;
	}
	command->Complete({}, nullptr);

	UpdateMediaPlayerTraitState();
}

void DeviceDaemon::OnMp3Pause(std::unique_ptr<weaved::Command> command)
{
	LOG(INFO) << "Pause MP3 playing...";
	if (!mp3_player_service_.get()) {
		command->Abort("_system_error", "MP3 player service unavailable", nullptr);
		return;
	}
	android::binder::Status status = mp3_player_service_->pause();
	if (!status.isOk()) {
		command->AbortWithCustomError(status, nullptr);
		return;
	}
	command->Complete({}, nullptr);

	UpdateMediaPlayerTraitState();
}

void DeviceDaemon::OnMp3Stop(std::unique_ptr<weaved::Command> command)
{
	LOG(INFO) << "Stop MP3 playing...";
	if (!mp3_player_service_.get()) {
		command->Abort("_system_error", "MP3 player service unavailable", nullptr);
		return;
	}
	android::binder::Status status = mp3_player_service_->stop();
	if (!status.isOk()) {
		command->AbortWithCustomError(status, nullptr);
		return;
	}
	command->Complete({}, nullptr);

	UpdateMediaPlayerTraitState();
}

int main(int argc, char* argv[])
{
	base::CommandLine::Init(argc, argv);
	brillo::InitLog(brillo::kLogToSyslog | brillo::kLogHeader);
	DeviceDaemon daemon;
	return daemon.Run();
}

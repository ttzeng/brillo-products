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
#include <dirent.h>
#include <sysexits.h>

#include <base/logging.h>
#include <base/command_line.h>
#include <base/macros.h>
#include <base/bind.h>
#include <binderwrapper/binder_wrapper.h>
#include <brillo/binder_watcher.h>
#include <brillo/daemons/daemon.h>
#include <brillo/syslog_logging.h>
#include <media/stagefright/AudioPlayer.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaExtractor.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/OMXClient.h>
#include <media/stagefright/SimpleDecodingSource.h>
#include <media/stagefright/ACodec.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/AudioSystem.h>
#include <include/MP3Extractor.h>

#include "mydevice/mp3player/BnMp3PlayerService.h"
#include "mp3-player-service.h"

using namespace android;

class Mp3PlayerService : public mydevice::mp3player::BnMp3PlayerService {
	const std::string SOUNDTRACKS_FORDER = "/data/soundtracks/";
	enum PlayerState {
		Idle,
		Playing,
		Paused,
	};
public:
	Mp3PlayerService() : player(nullptr), state(Idle) {
		status_t status = client.connect();
		if (status == OK)
			reloadPlaylist();
	}
	~Mp3PlayerService() {
		if (player) delete player;
	}
	android::binder::Status play();
	android::binder::Status pause();
	android::binder::Status stop();
	android::binder::Status reachedEOS(bool* pEOS);
	android::binder::Status status(String16* pInfo);
	android::binder::Status getVolume(float* pVol) {
		if (AudioSystem::getMasterVolume(pVol) != NO_ERROR)
			*pVol = 0.0f;
		return android::binder::Status::ok();
	}
	android::binder::Status setVolume(float vol) {
		AudioSystem::setMasterVolume(vol);
		return android::binder::Status::ok();
	}
	android::binder::Status isMuted(bool* pState) {
		AudioSystem::getMasterMute(pState);
		return android::binder::Status::ok();
	}
	android::binder::Status mute(bool state) {
		AudioSystem::setMasterMute(state);
		return android::binder::Status::ok();
	}
private:
	void reloadPlaylist();
	status_t PlayStagefrightMp3(std::string filename);

	OMXClient client;
	AudioPlayer* player;
	PlayerState state;
	std::vector<std::string> playList;
	size_t playIndex;
};

void Mp3PlayerService::reloadPlaylist()
{
	playList.clear();
	DIR *dp;
	if ((dp = opendir(SOUNDTRACKS_FORDER.c_str())) == NULL) {
		LOG(ERROR) << "Unable to open directory '" << SOUNDTRACKS_FORDER
		           << "' (errno=" << errno << ")";
		return;
	}
	struct dirent *dirp;
	while ((dirp = readdir(dp)) != NULL) {
		std::string filename(dirp->d_name);
		if (filename.find(".mp3") != std::string::npos)
			playList.push_back(filename);
	}
	closedir(dp);
	playIndex = 0;

	LOG(INFO) << "Found " << playList.size() << " MP3 files";
	for (size_t i = 0; i < playList.size(); i++)
		LOG(INFO) << "\t" << i << ": " << playList[i];
}

status_t Mp3PlayerService::PlayStagefrightMp3(std::string filename)
{
	/* ${BDK_PATH}/device/generic/brillo/pts/audio/brillo-audio-test/stagefright_playback.cpp */
	FileSource* file_source = new FileSource(filename.c_str());
	status_t status = file_source->initCheck();
	if (status != OK) {
		LOG(ERROR) << "Could not open the mp3 file source.";
		return status;
	}

	// Register default sniffers so MediaExtractor knows what kind of sniffer to
	// use.
	file_source->RegisterDefaultSniffers();

	// Extract media.
	sp<MediaExtractor> media_extractor =
		reinterpret_cast<android::MediaExtractor*>(MediaExtractor::Create(file_source, NULL).get());
	LOG(INFO) << "Num tracks: " << media_extractor->countTracks();
	sp<MediaSource> media_source =
		reinterpret_cast<android::MediaSource*>(media_extractor->getTrack(0).get());

	// Decode audio.
	sp<MediaSource> decoded_source = SimpleDecodingSource::Create(media_source);

	// Play audio.
	player = new AudioPlayer(nullptr);  // Initialize without source.
	player->setSource(decoded_source);
	status = player->start();
	if (status != OK) {
		LOG(ERROR) << "Could not start playing audio.";
		return status;
	}
	return status;
}

android::binder::Status Mp3PlayerService::play()
{
	switch (state) {
	case Idle:
		if (playIndex < playList.size() &&
		    PlayStagefrightMp3(SOUNDTRACKS_FORDER + playList[playIndex]) == OK)
			state = Playing;
	case Playing:
		break;
	case Paused:
		player->resume();
		state = Playing;
	}
	return android::binder::Status::ok();
}

android::binder::Status Mp3PlayerService::pause()
{
	if (state == Playing) {
		player->pause();
		state = Paused;
	}
	return android::binder::Status::ok();
}

android::binder::Status Mp3PlayerService::stop()
{
	if (state == Playing || state == Paused) {
		delete player;
		player = nullptr;
		state  = Idle;
		if (++playIndex >= playList.size())
			playIndex = 0;
	}
	return android::binder::Status::ok();
}

android::binder::Status Mp3PlayerService::reachedEOS(bool* pEOS)
{
	status_t s;
	*pEOS = (state == Playing && player->reachedEOS(&s));
	return android::binder::Status::ok();
}

android::binder::Status Mp3PlayerService::status(String16* pInfo)
{
	switch (state) {
	case Idle:
		pInfo->setTo(String16("idle"));
		break;
	case Playing:
		pInfo->setTo(String16(playList[playIndex].c_str()));
		break;
	case Paused:
		pInfo->setTo(String16("paused"));
		break;
	}
	return android::binder::Status::ok();
}

class MyDaemon final : public brillo::Daemon {
public:
	MyDaemon() = default;
protected:
	int OnInit() override;
private:
	/* the bridge between libbinder and brillo::MessageLoop */
	brillo::BinderWatcher binder_watcher_;

	android::sp<Mp3PlayerService> mp3_player_service_;

	base::WeakPtrFactory<MyDaemon> weak_ptr_factory_{this};
	DISALLOW_COPY_AND_ASSIGN(MyDaemon);
};

int MyDaemon::OnInit()
{
	int rc;
	if ((rc = brillo::Daemon::OnInit()) != EX_OK)
		return rc;

	/* Create and initialize the singleton for communicating with the real binder system */
	android::BinderWrapper::Create();
	if (!binder_watcher_.Init())
		return EX_OSERR;

	mp3_player_service_ = new Mp3PlayerService();
	android::BinderWrapper::Get()->RegisterService(mp3_player_service::kBinderServiceName,
	                                               mp3_player_service_);
	return EX_OK;
}

int main(int argc, char* argv[])
{
	base::CommandLine::Init(argc, argv);
	brillo::InitLog(brillo::kLogToSyslog | brillo::kLogHeader);
	MyDaemon daemon;
	return daemon.Run();
}

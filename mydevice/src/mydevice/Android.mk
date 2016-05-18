# Copyright 2015 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := mydevice
LOCAL_INIT_RC := mydevice.rc

LOCAL_REQUIRED_MODULES := \
	onOff.json \

LOCAL_CFLAGS := -Wall -Wno-unused-parameter -fexceptions

LOCAL_SRC_FILES :=	\
	mydevice.cpp	\

LOCAL_C_INCLUDES := \

LOCAL_SHARED_LIBRARIES := \
	libbinder \
	libbinderwrapper \
	libbrillo \
	libbrillo-binder \
	libbrillo-stream \
	libchrome \
	libhardware \
	libutils \
	libweaved \
	libmraa \

LOCAL_STATIC_LIBRARIES := \

include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_MODULE := onOff.json
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/weaved/traits
LOCAL_SRC_FILES := etc/weaved/traits/$(LOCAL_MODULE)

include $(BUILD_PREBUILT)

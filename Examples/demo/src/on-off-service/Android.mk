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
LOCAL_MODULE := on-off-service
LOCAL_INIT_RC := on-off-service.rc
LOCAL_REQUIRED_MODULES := on-off-service.json

LOCAL_CFLAGS := -Wall -Werror -Wno-unused-parameter -fexceptions

LOCAL_SRC_FILES :=	\
	on-off-service.cpp	\
	sketch.cpp \

LOCAL_SHARED_LIBRARIES := \
	libbinder \
	libbinderwrapper \
	libbrillo \
	libbrillo-binder \
	libbrillo-stream \
	libchrome \
	libhardware \
	libutils \
	libmraa \

LOCAL_STATIC_LIBRARIES := \
	libon-off-service \
	libarduino-mraa \

include $(BUILD_EXECUTABLE)

# Weave schema files
# ========================================================
include $(CLEAR_VARS)
LOCAL_MODULE := on-off-service.json
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/weaved/traits
LOCAL_SRC_FILES := etc/weaved/traits/$(LOCAL_MODULE)

include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := libon-off-service
LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/aidl
LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)
LOCAL_C_INCLUDES := \
	system/core/base/include \

LOCAL_SRC_FILES := \
	aidl/brillo/demo/IOnOffService.aidl \
	binder_constants.cpp \

include $(BUILD_STATIC_LIBRARY)

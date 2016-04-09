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
LOCAL_MODULE := libarduino-mraa

ARDUINO_CORES := $(LOCAL_PATH)/cores
ARDUINO_LIBRARY := $(LOCAL_PATH)/libraries

SRC_CORES := \
	$(wildcard $(ARDUINO_CORES)/*.c) $(wildcard $(ARDUINO_CORES)/*.cpp)
SRC_LIB_MAXMATRIX := \
	$(wildcard $(ARDUINO_LIBRARY)/MaxMatrix/*.cpp)
ALL_SRC := \
	$(SRC_CORES) \
	$(SRC_LIB_MAXMATRIX) \

LOCAL_C_INCLUDES := \
	$(ARDUINO_CORES) \

LOCAL_EXPORT_C_INCLUDE_DIRS := \
	$(ARDUINO_CORES) \
	$(ARDUINO_LIBRARY)/MaxMatrix \

LOCAL_SRC_FILES := $(ALL_SRC:$(LOCAL_PATH)/%=%)

LOCAL_CFLAGS := -Wall -Werror -Wno-unused-parameter -fexceptions

LOCAL_SHARED_LIBRARIES := \
	libmraa \

include $(BUILD_STATIC_LIBRARY)

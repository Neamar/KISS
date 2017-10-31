LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := KISSLauncher
LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := KISSLauncher

kiss_root  := $(LOCAL_PATH)
kiss_dir   := app
kiss_out   := $(PWD)/$(OUT_DIR)/target/common/obj/APPS/$(LOCAL_MODULE)_intermediates
kiss_build := $(kiss_root)/$(kiss_dir)/build
kiss_apk   := build/outputs/apk/$(kiss_dir)-release-unsigned.apk

$(kiss_root)/$(kiss_dir)/$(kiss_apk):
	rm -Rf $(kiss_build)
	mkdir -p $(kiss_out)
	ln -sf $(kiss_out) $(kiss_build)
	cd $(kiss_root)/$(kiss_dir) && gradle assembleRelease

LOCAL_CERTIFICATE := platform
LOCAL_SRC_FILES := $(kiss_dir)/$(kiss_apk)
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)

include $(BUILD_PREBUILT)

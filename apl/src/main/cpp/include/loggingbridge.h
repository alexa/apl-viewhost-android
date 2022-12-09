/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
#ifndef ANDROID_LOGGINGBRIDGE_H
#define ANDROID_LOGGINGBRIDGE_H

#include "apl/apl.h"
#include <android/log.h>

class AndroidJniLogBridge : public apl::LogBridge {
public:
    void transport(apl::LogLevel level, const std::string& log) override {
        __android_log_write(LEVEL_MAPPING.at(level), tag, log.c_str());
    }

private:
    const apl::Bimap<apl::LogLevel, int> LEVEL_MAPPING = {
            {apl::LogLevel::kTrace,    ANDROID_LOG_VERBOSE},
            {apl::LogLevel::kDebug,    ANDROID_LOG_DEBUG},
            {apl::LogLevel::kInfo,     ANDROID_LOG_INFO},
            {apl::LogLevel::kWarn,     ANDROID_LOG_WARN},
            {apl::LogLevel::kError,    ANDROID_LOG_ERROR},
            {apl::LogLevel::kCritical, ANDROID_LOG_FATAL}
    };
    const char *tag = "APL";
};

#endif // ANDROID_LOGGINGBRIDGE_H
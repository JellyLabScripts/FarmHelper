package com.jelly.farmhelper.config;

import cc.polyfrost.oneconfig.config.annotations.Text;

public class CustomFailsafeMessagesPage {
    @Text(
            name = "Rotation messages",
            description = "The messages to send to the chat when a rotation failsafe has been triggered (use '|' to split the messages)",
            placeholder = "Leave empty to use a random message",
            multiline = true
    )
    public static String customRotationMessages = "";

    @Text(
            name = "Bedrock messages",
            description = "The messages to send to the chat when a bedrock failsafe has been triggered (use '|' to split the messages)",
            placeholder = "Leave empty to use a random message",
            multiline = true
    )
    public static String customBedrockMessages = "";
}

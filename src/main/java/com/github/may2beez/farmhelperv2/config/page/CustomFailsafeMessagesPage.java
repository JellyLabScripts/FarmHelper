package com.github.may2beez.farmhelperv2.config.page;

import cc.polyfrost.oneconfig.config.annotations.Text;

public class CustomFailsafeMessagesPage {
    @Text(
            name = "Item change failsafe messages",
            description = "The messages to send to the chat when item change failsafe has been triggered (use '|' to split the messages)",
            placeholder = "Leave empty to use a random message",
            multiline = true
    )
    public static String customItemChangeMessages = "";
    @Text(
            name = "Rotation failsafe messages",
            description = "The messages to send to the chat when rotation failsafe has been triggered (use '|' to split the messages)",
            placeholder = "Leave empty to use a random message",
            multiline = true
    )
    public static String customRotationMessages = "";
    @Text(
            name = "Teleportation failsafe messages",
            description = "The messages to send to the chat when teleportation failsafe has been triggered (use '|' to split the messages)",
            placeholder = "Leave empty to use a random message",
            multiline = true
    )
    public static String customTeleportationMessages = "";

    @Text(
            name = "Bedrock failsafe messages",
            description = "The messages to send to the chat when bedrock failsafe has been triggered (use '|' to split the messages)",
            placeholder = "Leave empty to use a random message",
            multiline = true
    )
    public static String customBedrockMessages = "";

    @Text(
            name = "Dirt failsafe messages",
            description = "The messages to send to the chat when dirt failsafe has been triggered (use '|' to split the messages)",
            placeholder = "Leave empty to use a random message",
            multiline = true
    )
    public static String customDirtMessages = "";
}

package com.jelly.farmhelperv2.config.page;

import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Text;

/*
    Credits to Yuro for this superb class
*/
public class CustomFailsafeMessagesPage {
    @Text(
            name = "Custom messages sent during Jacob's Contest",
            description = "The messages to send to the chat when the failsafe has been triggered and you are during Jacob's Contest (use '|' to split the messages)",
            placeholder = "Leave empty to disable",
            multiline = true
    )
    public static String customJacobMessages = "";
    @Slider(
            name = "Custom Jacob's Contest message chance",
            description = "The chance that the custom Jacob's Contest message will be sent to the chat",
            min = 0,
            max = 100
    )
    public static int customJacobChance = 50;

    @Text(
            name = "Custom continue messages",
            description = "The messages to send to the chat when the failsafe has been triggered and you want to ask if you can continue (use '|' to split the messages)",
            placeholder = "Leave empty to use a random message",
            multiline = true
    )
    public static String customContinueMessages = "";
    @Text(
            name = "Rotation failsafe messages",
            description = "The messages to send to the chat when the rotation failsafe has been triggered (use '|' to split the messages)",
            placeholder = "Leave empty to use a random message",
            multiline = true
    )
    public static String customRotationMessages = "";
    @Text(
            name = "Teleportation failsafe messages",
            description = "The messages to send to the chat when the teleportation failsafe has been triggered (use '|' to split the messages)",
            placeholder = "Leave empty to use a random message",
            multiline = true
    )
    public static String customTeleportationMessages = "";
    @Text(
            name = "Knockback failsafe messages",
            description = "The messages to send to the chat when the knockback failsafe has been triggered (use '|' to split the messages)",
            placeholder = "Leave empty to use a random message",
            multiline = true
    )
    public static String customKnockbackMessages = "";

    @Text(
            name = "Bedrock failsafe messages",
            description = "The messages to send to the chat when the bedrock failsafe has been triggered (use '|' to split the messages)",
            placeholder = "Leave empty to use a random message",
            multiline = true
    )
    public static String customBedrockMessages = "";

    @Text(
            name = "Dirt failsafe messages",
            description = "The messages to send to the chat when the dirt failsafe has been triggered (use '|' to split the messages)",
            placeholder = "Leave empty to use a random message",
            multiline = true
    )
    public static String customDirtMessages = "";
}

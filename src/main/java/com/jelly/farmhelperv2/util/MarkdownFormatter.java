package com.jelly.farmhelperv2.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownFormatter {

    public static String format(String markdownText) {
        // Bold formatting
        markdownText = markdownText.replaceAll("\\*\\*(.*?)\\*\\*", "\u00A7l$1\u00A7r");

        // Italic formatting
        markdownText = markdownText.replaceAll("\\*(.*?)\\*", "\u00A7o$1\u00A7r");

        // Heading formatting
        markdownText = markdownText.replaceAll("(?m)^#{1,6}\\s+(.*?)$", "\u00A7n$1\u00A7r");

        // Link formatting (Display text)(URL)
        Pattern linkPattern = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");
        Matcher linkMatcher = linkPattern.matcher(markdownText);
        StringBuffer linkFormattedText = new StringBuffer();
        while (linkMatcher.find()) {
            String linkDisplayText = linkMatcher.group(1);
            String linkURL = linkMatcher.group(2);
            linkMatcher.appendReplacement(linkFormattedText, "\u00A7b" + linkDisplayText + "\u00A7r");
            linkFormattedText.append(" (").append(linkURL).append(")");
        }
        linkMatcher.appendTail(linkFormattedText);
        markdownText = linkFormattedText.toString();

        // Code block formatting (``` code ```)
        markdownText = markdownText.replaceAll("```([^`]+)```", "\u00A77\u00A7o$1\u00A77\u00A7r");

        // Unordered list formatting
        markdownText = markdownText.replaceAll("(?m)^\\*\\s+(.*?)$", "\u2022 $1");
        markdownText = markdownText.replaceAll("(?m)^(\\s*\\*)\\s+(.*?)$", "    \u25E6 $2");

        // Ordered list formatting
        Pattern orderedListPattern = Pattern.compile("(?m)^(\\d+)\\.\\s+(.*?)$");
        Matcher orderedListMatcher = orderedListPattern.matcher(markdownText);
        StringBuffer orderedListFormattedText = new StringBuffer();
        while (orderedListMatcher.find()) {
            String listItemNumber = orderedListMatcher.group(1);
            String listItemText = orderedListMatcher.group(2);
            orderedListMatcher.appendReplacement(orderedListFormattedText, "\u00A7l" + listItemNumber + ".\u00A7r " + listItemText);
        }
        orderedListMatcher.appendTail(orderedListFormattedText);
        markdownText = orderedListFormattedText.toString();

        return markdownText;
    }
}

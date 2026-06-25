package com.example.urticare20.model

data class ChatMessage(
    val sender: String, // "User" or "Urti"
    val text: String,
    val timestamp: String
)

data class ChatSession(
    val id: String,
    val title: String,
    val startTime: String,
    val messages: List<ChatMessage>
)

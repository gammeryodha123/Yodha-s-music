package com.example.data

import kotlinx.coroutines.delay

data class LyricLine(
    val timeMs: Long,
    val text: String
)

class LyricsRepository {
    // Return a list of LyricLines with some simulated network delay
    suspend fun fetchLyrics(songId: String): List<LyricLine> {
        delay(600) // Simulated network fetch delay
        return when (songId) {
            "1" -> listOf(
                LyricLine(0L, "🎵 (Instrumental Intro) 🎵"),
                LyricLine(8000L, "Cruising through the midnight rain 🌧️"),
                LyricLine(14000L, "Reflections of the neon lights in vain"),
                LyricLine(20000L, "Shadows whispering my name"),
                LyricLine(26000L, "We are fading into the night so bright ✨"),
                LyricLine(32000L, "Oh, neon dreams, take me away"),
                LyricLine(38000L, "To the place where the synth-wave plays 🎸"),
                LyricLine(44000L, "No more sorrows, no more pain"),
                LyricLine(50000L, "Just the rhythm running through our veins"),
                LyricLine(56000L, "⚡ (Synth Solo) ⚡"),
                LyricLine(70000L, "Driving past the city line 🏎️"),
                LyricLine(76000L, "A digital horizon in our eyes"),
                LyricLine(82000L, "Moving to a beat in time ⏱️"),
                LyricLine(88000L, "Underneath the simulated skies"),
                LyricLine(94000L, "Oh, neon dreams, take me away"),
                LyricLine(100000L, "To the place where the synth-wave plays 🎸"),
                LyricLine(106000L, "No more sorrows, no more pain"),
                LyricLine(112000L, "Just the rhythm running through our veins"),
                LyricLine(118000L, "🎵 (Outro Beats) 🎵")
            )
            "2" -> listOf(
                LyricLine(0L, "🌅 (Acoustic Guitar Intro) 🌅"),
                LyricLine(6000L, "Golden rays breaking through the trees 🌲"),
                LyricLine(12000L, "Waking up to the morning breeze"),
                LyricLine(18000L, "All the worries of yesterday fade"),
                LyricLine(24000L, "In this quiet light we have made ☕"),
                LyricLine(30000L, "Acoustic sunrise, guide me home 🧭"),
                LyricLine(36000L, "Wherever my restless heart may roam"),
                LyricLine(42000L, "A gentle hum of a brand new day"),
                LyricLine(48000L, "Chasing all of the dark away"),
                LyricLine(54000L, "🍃 (Acoustic Solo) 🍃"),
                LyricLine(68000L, "Step by step on the dewy grass 🌿"),
                LyricLine(74000L, "Watching the shadows slowly pass"),
                LyricLine(80000L, "No rush, no noise, just peace"),
                LyricLine(86000L, "May this feeling never cease"),
                LyricLine(92000L, "Acoustic sunrise, guide me home 🧭"),
                LyricLine(98000L, "Wherever my restless heart may roam"),
                LyricLine(104000L, "A gentle hum of a brand new day"),
                LyricLine(110000L, "Chasing all of the dark away"),
                LyricLine(116000L, "🌅 (Guitar Outro Fade Out) 🌅")
            )
            "3" -> listOf(
                LyricLine(0L, "⚡ (Industrial Synth Intro) ⚡"),
                LyricLine(10000L, "Neon towers pierce the neon sky 🏙️"),
                LyricLine(16000L, "People in the shadows passing by"),
                LyricLine(22000L, "A million wires running deep inside"),
                LyricLine(28000L, "There is nowhere left for us to hide"),
                LyricLine(34000L, "In the cyberpunk echoes we thrive 💾"),
                LyricLine(40000L, "Trying to keep the human soul alive"),
                LyricLine(46000L, "A digital voice in a concrete maze"),
                LyricLine(52000L, "Lost inside this high-tech haze 🌫️"),
                LyricLine(58000L, "🕹️ (Glitch Beat Interlude) 🕹️"),
                LyricLine(70000L, "System reboot, memory clear"),
                LyricLine(76000L, "But the melody is all I hear"),
                LyricLine(82000L, "Through the static, through the stream 📡"),
                LyricLine(88000L, "We are chasing an electric dream"),
                LyricLine(94000L, "In the cyberpunk echoes we thrive 💾"),
                LyricLine(100000L, "Trying to keep the human soul alive"),
                LyricLine(106000L, "A digital voice in a concrete maze"),
                LyricLine(112000L, "Lost inside this high-tech haze"),
                LyricLine(118000L, "⚡ (Glitch Beats Outro) ⚡")
            )
            "4" -> listOf(
                LyricLine(0L, "☕ (Vinyl Crackle & Chill Keys) ☕"),
                LyricLine(8000L, "Raindrops tapping on the window pane 🌧️"),
                LyricLine(15000L, "Lofi beats to soothe the brain"),
                LyricLine(22000L, "A cup of coffee, a notebook open wide 📝"),
                LyricLine(29000L, "Finding cozy warmth inside"),
                LyricLine(36000L, "Just let the record spin around 🎶"),
                LyricLine(43000L, "Lost inside this soothing sound"),
                LyricLine(50000L, "No thoughts, just drifting free ☁️"),
                LyricLine(57000L, "Exactly where I want to be"),
                LyricLine(64000L, "🍂 (Instrumental Beat Break) 🍂"),
                LyricLine(80000L, "Daydreams floating in the air"),
                LyricLine(87000L, "Letting go of every single care"),
                LyricLine(94000L, "With the lofi beats we find our pace"),
                LyricLine(101000L, "A calm and quiet, happy space 🏡"),
                LyricLine(108000L, "Just let the record spin around 🎶"),
                LyricLine(115000L, "Lost inside this soothing sound"),
                LyricLine(122000L, "☕ (Vinyl Crackle Outro) ☕")
            )
            else -> {
                generateDynamicLyrics(songId)
            }
        }
    }

    private fun generateDynamicLyrics(songId: String): List<LyricLine> {
        val seed = songId.hashCode()
        val list = mutableListOf<LyricLine>()
        list.add(LyricLine(0L, "🎵 (Instrumental Intro) 🎵"))
        
        val phrases = listOf(
            "Welcome to this beautiful melody",
            "Feel the rhythm, let it set you free ✨",
            "Walking down the familiar street",
            "To the tempo of your heartbeat 💓",
            "Every note tells a story of its own",
            "A journey into the great unknown 🌌",
            "We are singing our hearts out tonight",
            "Everything is going to be alright",
            "Under the golden, shining stars ⭐",
            "Healed from all our old scars"
        )
        
        var currentTime = 6000L
        for (i in 0 until 12) {
            val phrase = phrases[Math.abs((seed + i) % phrases.size)]
            list.add(LyricLine(currentTime, phrase))
            currentTime += 6000L + Math.abs(seed % 3000L)
        }
        
        list.add(LyricLine(currentTime, "🎵 (Outro Fade) 🎵"))
        return list
    }
}

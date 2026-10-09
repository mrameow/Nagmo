package com.nagmo.app.alarm

import com.nagmo.app.data.Personality
import kotlin.random.Random

/** The words Nagmo uses, by personality. */
object NagMessages {
    private val alarm = mapOf(
        Personality.GENTLE to listOf(
            "Hey friend, it's time for this one",
            "A little reminder, no pressure",
            "You've got this",
            "Gentle nudge from Nagmo",
        ),
        Personality.SASSY to listOf(
            "Ahem. Remember this?",
            "I'm not mad, just… nagging",
            "Your future self says thanks",
            "Knock knock. It's your to-do",
            "Nagmo here. Still waiting…",
            "You said you'd do it. I wrote it down",
        ),
        Personality.DRILL to listOf(
            "ATTENTION! DO IT NOW!",
            "DROP EVERYTHING",
            "NO EXCUSES, RECRUIT!",
            "MOVE IT, MOVE IT, MOVE IT!",
        ),
    )

    private val renag = mapOf(
        Personality.GENTLE to listOf("Just checking in again", "Still on your list, whenever you're ready"),
        Personality.SASSY to listOf("Me again. Surprised?", "Did you think I'd forget?", "Nag #%d. I can do this all day."),
        Personality.DRILL to listOf("I SAID NOW!", "NAG #%d! WHY IS THIS NOT DONE?!"),
    )

    private val deadline = mapOf(
        Personality.GENTLE to "Heads-up: this is due %s",
        Personality.SASSY to "Tick tock. Due %s",
        Personality.DRILL to "DEADLINE %s! DOUBLE TIME!",
    )

    private val done = mapOf(
        Personality.GENTLE to listOf("Wonderful, well done!", "Proud of you", "One less thing!"),
        Personality.SASSY to listOf("Look at you, being productive!", "Fine. I'll stop nagging. For now", "Nagmo approves"),
        Personality.DRILL to listOf("ACCEPTABLE, RECRUIT!", "MISSION COMPLETE. NEXT!", "OUTSTANDING!"),
    )

    fun alarmTitle(p: Personality, nagCount: Int): String =
        if (nagCount <= 1) alarm.getValue(p).random()
        else renag.getValue(p).random().let { if (it.contains("%d")) it.format(nagCount) else it }

    fun deadlineTitle(p: Personality, whenText: String) = deadline.getValue(p).format(whenText)

    fun doneCheer(p: Personality) = done.getValue(p).random()

    fun digestTitle(p: Personality, count: Int): String = when (p) {
        Personality.GENTLE -> if (count == 1) "Good morning! 1 thing today" else "Good morning! $count things today"
        Personality.SASSY -> if (count == 1) "Rise and shine, 1 nag awaits" else "Rise and shine, $count nags await"
        Personality.DRILL -> "REVEILLE! $count TASKS TODAY!"
    }

    fun emptyLine(p: Personality): String = when (p) {
        Personality.GENTLE -> "All clear. Enjoy a calm moment"
        Personality.SASSY -> "Nothing to nag about… suspicious"
        Personality.DRILL -> "NO TASKS? FIND SOME, RECRUIT!"
    }

    fun tagline(): String = listOf(
        "We nag so you don't have to.",
        "Your personalised nagging memo.",
        "Sticky notes with opinions.",
    )[Random.nextInt(3)]
}

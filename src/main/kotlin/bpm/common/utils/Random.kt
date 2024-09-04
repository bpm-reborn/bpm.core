package bpm.common.utils

import java.util.*
import kotlin.math.ln
import kotlin.random.Random

object Random { private data class Word(val value: String, val type: WordType)


    private enum class WordType {
        // Basic word types
        NOUN, VERB, ADJECTIVE, ADVERB, PREPOSITION, CONJUNCTION, PRONOUN, DETERMINER,
        INTERJECTION, FILLER, SIMILE, COMMA,

        // Historical word types
        HISTORICAL_NOUN, HISTORICAL_VERB, HISTORICAL_FIGURE, HISTORICAL_ERA,
        HISTORICAL_LOCATION, HISTORICAL_PHRASE,

        // Romantic word types
        ROMANTIC_NOUN, ROMANTIC_VERB, ROMANTIC_ADJECTIVE, ROMANTIC_LOCATION,
        ROMANTIC_BODY_PART, ROMANTIC_PHRASE,

        // Horror word types
        HORROR_NOUN, HORROR_VERB, HORROR_CREATURE, HORROR_LOCATION, HORROR_SOUND,
        HORROR_BODY_PART, HORROR_DESCRIPTION, HORROR_PHRASE,

        // Mystery word types
        MYSTERY_NOUN, MYSTERY_VERB, MYSTERY_DETECTIVE, MYSTERY_CLUE, MYSTERY_LOCATION,
        MYSTERY_EVIDENCE, MYSTERY_SUSPECT, MYSTERY_ALIBI, MYSTERY_PHRASE,

        // Western word types
        WESTERN_NOUN, WESTERN_VERB, WESTERN_CHARACTER, WESTERN_LOCATION, WESTERN_TIME,
        WESTERN_ITEM, WESTERN_EXCLAMATION, WESTERN_PHRASE,

        // Teen ADHD word types
        TEEN_INTERJECTION, TEEN_FILLER,

        // Fantasy word types
        FANTASY_NOUN, FANTASY_VERB,

        // Cyberpunk word types
        CYBERPUNK_NOUN, CYBERPUNK_VERB,

        // Additional general types
        POSSESSIVE
    }


    private enum class Tense { PRESENT, PAST, FUTURE }

    private val wordList = listOf(
        // Nouns (200)
        Word("time", WordType.NOUN),
        Word("year", WordType.NOUN),
        Word("people", WordType.NOUN),
        Word("way", WordType.NOUN),
        Word("day", WordType.NOUN),
        Word("man", WordType.NOUN),
        Word("thing", WordType.NOUN),
        Word("woman", WordType.NOUN),
        Word("life", WordType.NOUN),
        Word("child", WordType.NOUN),
        Word("world", WordType.NOUN),
        Word("school", WordType.NOUN),
        Word("state", WordType.NOUN),
        Word("family", WordType.NOUN),
        Word("student", WordType.NOUN),
        Word("group", WordType.NOUN),
        Word("country", WordType.NOUN),
        Word("problem", WordType.NOUN),
        Word("hand", WordType.NOUN),
        Word("part", WordType.NOUN),
        Word("place", WordType.NOUN),
        Word("case", WordType.NOUN),
        Word("week", WordType.NOUN),
        Word("company", WordType.NOUN),
        Word("system", WordType.NOUN),
        Word("program", WordType.NOUN),
        Word("question", WordType.NOUN),
        Word("work", WordType.NOUN),
        Word("government", WordType.NOUN),
        Word("number", WordType.NOUN),
        Word("night", WordType.NOUN),
        Word("point", WordType.NOUN),
        Word("home", WordType.NOUN),
        Word("water", WordType.NOUN),
        Word("room", WordType.NOUN),
        Word("mother", WordType.NOUN),
        Word("area", WordType.NOUN),
        Word("money", WordType.NOUN),
        Word("story", WordType.NOUN),
        Word("fact", WordType.NOUN),
        Word("month", WordType.NOUN),
        Word("lot", WordType.NOUN),
        Word("right", WordType.NOUN),
        Word("study", WordType.NOUN),
        Word("book", WordType.NOUN),
        Word("eye", WordType.NOUN),
        Word("job", WordType.NOUN),
        Word("word", WordType.NOUN),
        Word("business", WordType.NOUN),
        Word("issue", WordType.NOUN),
        Word("side", WordType.NOUN),
        Word("kind", WordType.NOUN),
        Word("head", WordType.NOUN),
        Word("house", WordType.NOUN),
        Word("service", WordType.NOUN),
        Word("friend", WordType.NOUN),
        Word("father", WordType.NOUN),
        Word("power", WordType.NOUN),
        Word("hour", WordType.NOUN),
        Word("game", WordType.NOUN),
        Word("line", WordType.NOUN),
        Word("end", WordType.NOUN),
        Word("member", WordType.NOUN),
        Word("law", WordType.NOUN),
        Word("car", WordType.NOUN),
        Word("city", WordType.NOUN),
        Word("community", WordType.NOUN),
        Word("name", WordType.NOUN),
        Word("president", WordType.NOUN),
        Word("team", WordType.NOUN),
        Word("minute", WordType.NOUN),
        Word("idea", WordType.NOUN),
        Word("kid", WordType.NOUN),
        Word("body", WordType.NOUN),
        Word("information", WordType.NOUN),
        Word("back", WordType.NOUN),
        Word("parent", WordType.NOUN),
        Word("face", WordType.NOUN),
        Word("others", WordType.NOUN),
        Word("level", WordType.NOUN),
        Word("office", WordType.NOUN),
        Word("door", WordType.NOUN),
        Word("health", WordType.NOUN),
        Word("person", WordType.NOUN),
        Word("art", WordType.NOUN),
        Word("war", WordType.NOUN),
        Word("history", WordType.NOUN),
        Word("party", WordType.NOUN),
        Word("result", WordType.NOUN),
        Word("change", WordType.NOUN),
        Word("morning", WordType.NOUN),
        Word("reason", WordType.NOUN),
        Word("research", WordType.NOUN),
        Word("girl", WordType.NOUN),
        Word("guy", WordType.NOUN),
        Word("moment", WordType.NOUN),
        Word("air", WordType.NOUN),
        Word("teacher", WordType.NOUN),
        Word("force", WordType.NOUN),
        Word("education", WordType.NOUN),
        Word("foot", WordType.NOUN),
        Word("boy", WordType.NOUN),
        Word("age", WordType.NOUN),
        Word("policy", WordType.NOUN),
        Word("process", WordType.NOUN),
        Word("music", WordType.NOUN),
        Word("market", WordType.NOUN),
        Word("sense", WordType.NOUN),
        Word("nation", WordType.NOUN),
        Word("plan", WordType.NOUN),
        Word("college", WordType.NOUN),
        Word("interest", WordType.NOUN),
        Word("death", WordType.NOUN),
        Word("experience", WordType.NOUN),
        Word("effect", WordType.NOUN),
        Word("movie", WordType.NOUN),
        Word("support", WordType.NOUN),
        Word("data", WordType.NOUN),
        Word("approach", WordType.NOUN),
        Word("opportunity", WordType.NOUN),
        Word("film", WordType.NOUN),
        Word("technology", WordType.NOUN),
        Word("road", WordType.NOUN),
        Word("role", WordType.NOUN),
        Word("ohio", WordType.NOUN),
        Word("baby gronk", WordType.NOUN),
        Word("fanum tax", WordType.NOUN),
        Word("practice", WordType.NOUN),
        Word("street", WordType.NOUN),
        Word("court", WordType.NOUN),
        Word("ground", WordType.NOUN),
        Word("police", WordType.NOUN),
        Word("site", WordType.NOUN),
        Word("town", WordType.NOUN),
        Word("stage", WordType.NOUN),
        Word("tree", WordType.NOUN),
        Word("race", WordType.NOUN),
        Word("food", WordType.NOUN),
        Word("sun", WordType.NOUN),
        Word("land", WordType.NOUN),
        Word("wall", WordType.NOUN),
        Word("crime", WordType.NOUN),
        Word("tax", WordType.NOUN),
        Word("tea", WordType.NOUN),
        Word("dinner", WordType.NOUN),
        Word("dog", WordType.NOUN),
        Word("chair", WordType.NOUN),
        Word("river", WordType.NOUN),
        Word("doctor", WordType.NOUN),
        Word("lake", WordType.NOUN),
        Word("animal", WordType.NOUN),
        Word("north", WordType.NOUN),
        Word("love", WordType.NOUN),
        Word("sport", WordType.NOUN),
        Word("clock", WordType.NOUN),
        Word("dream", WordType.NOUN),
        Word("bird", WordType.NOUN),
        Word("radio", WordType.NOUN),
        Word("window", WordType.NOUN),
        Word("fish", WordType.NOUN),
        Word("horse", WordType.NOUN),
        Word("ship", WordType.NOUN),
        Word("stone", WordType.NOUN),
        Word("village", WordType.NOUN),
        Word("farm", WordType.NOUN),
        Word("season", WordType.NOUN),
        Word("hill", WordType.NOUN),
        Word("island", WordType.NOUN),
        Word("wood", WordType.NOUN),
        Word("bridge", WordType.NOUN),
        Word("fire", WordType.NOUN),
        Word("forest", WordType.NOUN),
        Word("beach", WordType.NOUN),
        Word("garden", WordType.NOUN),
        Word("sea", WordType.NOUN),
        Word("field", WordType.NOUN),
        Word("plane", WordType.NOUN),
        Word("mountain", WordType.NOUN),
        Word("pool", WordType.NOUN),
        Word("plant", WordType.NOUN),
        Word("star", WordType.NOUN),
        Word("rain", WordType.NOUN),
        Word("snow", WordType.NOUN),
        Word("flower", WordType.NOUN),
        Word("valley", WordType.NOUN),
        Word("ocean", WordType.NOUN),
        Word("wind", WordType.NOUN),
        Word("moon", WordType.NOUN),
        Word("sky", WordType.NOUN),

        // Verbs (200)
        Word("be", WordType.VERB),
        Word("have", WordType.VERB),
        Word("do", WordType.VERB),
        Word("say", WordType.VERB),
        Word("go", WordType.VERB),
        Word("can", WordType.VERB),
        Word("get", WordType.VERB),
        Word("would", WordType.VERB),
        Word("make", WordType.VERB),
        Word("know", WordType.VERB),
        Word("will", WordType.VERB),
        Word("think", WordType.VERB),
        Word("take", WordType.VERB),
        Word("see", WordType.VERB),
        Word("come", WordType.VERB),
        Word("could", WordType.VERB),
        Word("want", WordType.VERB),
        Word("look", WordType.VERB),
        Word("use", WordType.VERB),
        Word("find", WordType.VERB),
        Word("give", WordType.VERB),
        Word("tell", WordType.VERB),
        Word("work", WordType.VERB),
        Word("may", WordType.VERB),
        Word("should", WordType.VERB),
        Word("call", WordType.VERB),
        Word("try", WordType.VERB),
        Word("ask", WordType.VERB),
        Word("need", WordType.VERB),
        Word("feel", WordType.VERB),
        Word("become", WordType.VERB),
        Word("leave", WordType.VERB),
        Word("put", WordType.VERB),
        Word("mean", WordType.VERB),
        Word("keep", WordType.VERB),
        Word("let", WordType.VERB),
        Word("begin", WordType.VERB),
        Word("seem", WordType.VERB),
        Word("help", WordType.VERB),
        Word("talk", WordType.VERB),
        Word("turn", WordType.VERB),
        Word("start", WordType.VERB),
        Word("might", WordType.VERB),
        Word("show", WordType.VERB),
        Word("hear", WordType.VERB),
        Word("play", WordType.VERB),
        Word("run", WordType.VERB),
        Word("move", WordType.VERB),
        Word("like", WordType.VERB),
        Word("live", WordType.VERB),
        Word("believe", WordType.VERB),
        Word("hold", WordType.VERB),
        Word("bring", WordType.VERB),
        Word("happen", WordType.VERB),
        Word("must", WordType.VERB),
        Word("write", WordType.VERB),
        Word("provide", WordType.VERB),
        Word("sit", WordType.VERB),
        Word("stand", WordType.VERB),
        Word("lose", WordType.VERB),
        Word("pay", WordType.VERB),
        Word("meet", WordType.VERB),
        Word("include", WordType.VERB),
        Word("continue", WordType.VERB),
        Word("set", WordType.VERB),
        Word("learn", WordType.VERB),
        Word("change", WordType.VERB),
        Word("lead", WordType.VERB),
        Word("understand", WordType.VERB),
        Word("watch", WordType.VERB),
        Word("follow", WordType.VERB),
        Word("stop", WordType.VERB),
        Word("create", WordType.VERB),
        Word("speak", WordType.VERB),
        Word("read", WordType.VERB),
        Word("allow", WordType.VERB),
        Word("add", WordType.VERB),
        Word("spend", WordType.VERB),
        Word("grow", WordType.VERB),
        Word("open", WordType.VERB),
        Word("walk", WordType.VERB),
        Word("win", WordType.VERB),
        Word("offer", WordType.VERB),
        Word("remember", WordType.VERB),
        Word("love", WordType.VERB),
        Word("consider", WordType.VERB),
        Word("appear", WordType.VERB),
        Word("buy", WordType.VERB),
        Word("wait", WordType.VERB),
        Word("serve", WordType.VERB),
        Word("die", WordType.VERB),
        Word("send", WordType.VERB),
        Word("expect", WordType.VERB),
        Word("build", WordType.VERB),
        Word("stay", WordType.VERB),
        Word("fall", WordType.VERB),
        Word("cut", WordType.VERB),
        Word("reach", WordType.VERB),
        Word("kill", WordType.VERB),
        Word("remain", WordType.VERB),
        Word("suggest", WordType.VERB),
        Word("raise", WordType.VERB),
        Word("pass", WordType.VERB),
        Word("sell", WordType.VERB),
        Word("require", WordType.VERB),
        Word("report", WordType.VERB),
        Word("decide", WordType.VERB),
        Word("pull", WordType.VERB),
        Word("rise", WordType.VERB),
        Word("receive", WordType.VERB),
        Word("visit", WordType.VERB),
        Word("cause", WordType.VERB),
        Word("drink", WordType.VERB),
        Word("drive", WordType.VERB),
        Word("eat", WordType.VERB),
        Word("cover", WordType.VERB),
        Word("break", WordType.VERB),
        Word("describe", WordType.VERB),
        Word("fill", WordType.VERB),
        Word("join", WordType.VERB),
        Word("produce", WordType.VERB),
        Word("choose", WordType.VERB),
        Word("develop", WordType.VERB),
        Word("hope", WordType.VERB),
        Word("sleep", WordType.VERB),
        Word("carry", WordType.VERB),
        Word("define", WordType.VERB),
        Word("wish", WordType.VERB),
        Word("involve", WordType.VERB),
        Word("hit", WordType.VERB),
        Word("push", WordType.VERB),
        Word("seek", WordType.VERB),
        Word("support", WordType.VERB),
        Word("throw", WordType.VERB),
        Word("catch", WordType.VERB),
        Word("lift", WordType.VERB),
        Word("study", WordType.VERB),
        Word("pick", WordType.VERB),
        Word("drop", WordType.VERB),
        Word("argue", WordType.VERB),
        Word("wear", WordType.VERB),
        Word("improve", WordType.VERB),
        Word("handle", WordType.VERB),
        Word("accept", WordType.VERB),
        Word("enter", WordType.VERB),
        Word("enjoy", WordType.VERB),
        Word("divide", WordType.VERB),
        Word("jump", WordType.VERB),
        Word("publish", WordType.VERB),
        Word("prevent", WordType.VERB),
        Word("invite", WordType.VERB),
        Word("rizzed", WordType.VERB),
        Word("sing", WordType.VERB),
        Word("dance", WordType.VERB),
        Word("solve", WordType.VERB),
        Word("spread", WordType.VERB),
        Word("reduce", WordType.VERB),
        Word("order", WordType.VERB),
        Word("collect", WordType.VERB),
        Word("prefer", WordType.VERB),
        Word("insist", WordType.VERB),
        Word("protect", WordType.VERB),
        Word("warn", WordType.VERB),
        Word("maintain", WordType.VERB),
        Word("relate", WordType.VERB),
        Word("trust", WordType.VERB),
        Word("select", WordType.VERB),
        Word("indicate", WordType.VERB),
        Word("arrange", WordType.VERB),
        Word("survive", WordType.VERB),
        Word("combine", WordType.VERB),
        Word("approve", WordType.VERB),
        Word("achieve", WordType.VERB),
        Word("influence", WordType.VERB),
        Word("attend", WordType.VERB),
        // Continuing from where we left off
        Word("gain", WordType.VERB),
        Word("cook", WordType.VERB),
        Word("wash", WordType.VERB),
        Word("remove", WordType.VERB),
        Word("maintain", WordType.VERB),
        Word("enable", WordType.VERB),
        Word("engage", WordType.VERB),
        Word("obtain", WordType.VERB),
        Word("teach", WordType.VERB),
        Word("fly", WordType.VERB),
        Word("adapt", WordType.VERB),
        Word("announce", WordType.VERB),
        Word("praise", WordType.VERB),
        Word("undertake", WordType.VERB),
        Word("complete", WordType.VERB),

        // Adjectives (200)
        Word("good", WordType.ADJECTIVE),
        Word("new", WordType.ADJECTIVE),
        Word("first", WordType.ADJECTIVE),
        Word("last", WordType.ADJECTIVE),
        Word("long", WordType.ADJECTIVE),
        Word("great", WordType.ADJECTIVE),
        Word("little", WordType.ADJECTIVE),
        Word("own", WordType.ADJECTIVE),
        Word("other", WordType.ADJECTIVE),
        Word("old", WordType.ADJECTIVE),
        Word("right", WordType.ADJECTIVE),
        Word("big", WordType.ADJECTIVE),
        Word("high", WordType.ADJECTIVE),
        Word("different", WordType.ADJECTIVE),
        Word("small", WordType.ADJECTIVE),
        Word("large", WordType.ADJECTIVE),
        Word("next", WordType.ADJECTIVE),
        Word("early", WordType.ADJECTIVE),
        Word("young", WordType.ADJECTIVE),
        Word("rizzler", WordType.ADJECTIVE),
        Word("skibbidi", WordType.ADJECTIVE),
        Word("important", WordType.ADJECTIVE),
        Word("few", WordType.ADJECTIVE),
        Word("public", WordType.ADJECTIVE),
        Word("bad", WordType.ADJECTIVE),
        Word("same", WordType.ADJECTIVE),
        Word("able", WordType.ADJECTIVE),
        Word("free", WordType.ADJECTIVE),
        Word("best", WordType.ADJECTIVE),
        Word("better", WordType.ADJECTIVE),
        Word("true", WordType.ADJECTIVE),
        Word("sure", WordType.ADJECTIVE),
        Word("black", WordType.ADJECTIVE),
        Word("white", WordType.ADJECTIVE),
        Word("real", WordType.ADJECTIVE),
        Word("local", WordType.ADJECTIVE),
        Word("simple", WordType.ADJECTIVE),
        Word("open", WordType.ADJECTIVE),
        Word("clear", WordType.ADJECTIVE),
        Word("full", WordType.ADJECTIVE),
        Word("far", WordType.ADJECTIVE),
        Word("late", WordType.ADJECTIVE),
        Word("low", WordType.ADJECTIVE),
        Word("major", WordType.ADJECTIVE),
        Word("current", WordType.ADJECTIVE),
        Word("beautiful", WordType.ADJECTIVE),
        Word("happy", WordType.ADJECTIVE),
        Word("serious", WordType.ADJECTIVE),
        Word("ready", WordType.ADJECTIVE),
        Word("short", WordType.ADJECTIVE),
        Word("special", WordType.ADJECTIVE),
        Word("strong", WordType.ADJECTIVE),
        Word("poor", WordType.ADJECTIVE),
        Word("natural", WordType.ADJECTIVE),
        Word("nice", WordType.ADJECTIVE),
        Word("private", WordType.ADJECTIVE),
        Word("hard", WordType.ADJECTIVE),
        Word("past", WordType.ADJECTIVE),
        Word("central", WordType.ADJECTIVE),
        Word("close", WordType.ADJECTIVE),
        Word("common", WordType.ADJECTIVE),
        Word("legal", WordType.ADJECTIVE),
        Word("red", WordType.ADJECTIVE),
        Word("blue", WordType.ADJECTIVE),
        Word("green", WordType.ADJECTIVE),
        Word("difficult", WordType.ADJECTIVE),
        Word("effective", WordType.ADJECTIVE),
        Word("popular", WordType.ADJECTIVE),
        Word("available", WordType.ADJECTIVE),
        Word("similar", WordType.ADJECTIVE),
        Word("recent", WordType.ADJECTIVE),
        Word("heavy", WordType.ADJECTIVE),
        Word("basic", WordType.ADJECTIVE),
        Word("various", WordType.ADJECTIVE),
        Word("usual", WordType.ADJECTIVE),
        Word("hot", WordType.ADJECTIVE),
        Word("cold", WordType.ADJECTIVE),
        Word("final", WordType.ADJECTIVE),
        Word("main", WordType.ADJECTIVE),
        Word("potential", WordType.ADJECTIVE),
        Word("specific", WordType.ADJECTIVE),
        Word("dark", WordType.ADJECTIVE),
        Word("deep", WordType.ADJECTIVE),
        Word("wide", WordType.ADJECTIVE),
        Word("easy", WordType.ADJECTIVE),
        Word("safe", WordType.ADJECTIVE),
        Word("significant", WordType.ADJECTIVE),
        Word("interesting", WordType.ADJECTIVE),
        Word("huge", WordType.ADJECTIVE),
        Word("rich", WordType.ADJECTIVE),
        Word("wonderful", WordType.ADJECTIVE),
        Word("quick", WordType.ADJECTIVE),
        Word("fresh", WordType.ADJECTIVE),
        Word("warm", WordType.ADJECTIVE),
        Word("soft", WordType.ADJECTIVE),
        Word("slow", WordType.ADJECTIVE),
        Word("clean", WordType.ADJECTIVE),
        Word("bright", WordType.ADJECTIVE),
        Word("dry", WordType.ADJECTIVE),
        Word("strange", WordType.ADJECTIVE),
        Word("cool", WordType.ADJECTIVE),
        Word("loud", WordType.ADJECTIVE),
        Word("calm", WordType.ADJECTIVE),
        Word("thick", WordType.ADJECTIVE),
        Word("thin", WordType.ADJECTIVE),
        Word("weak", WordType.ADJECTIVE),
        Word("wet", WordType.ADJECTIVE),
        Word("sick", WordType.ADJECTIVE),
        Word("wild", WordType.ADJECTIVE),
        Word("smooth", WordType.ADJECTIVE),
        Word("sharp", WordType.ADJECTIVE),
        Word("tight", WordType.ADJECTIVE),
        Word("flat", WordType.ADJECTIVE),
        Word("sweet", WordType.ADJECTIVE),
        Word("sour", WordType.ADJECTIVE),
        Word("bitter", WordType.ADJECTIVE),
        Word("loose", WordType.ADJECTIVE),
        Word("cheap", WordType.ADJECTIVE),
        Word("famous", WordType.ADJECTIVE),
        Word("brave", WordType.ADJECTIVE),
        Word("calm", WordType.ADJECTIVE),
        Word("busy", WordType.ADJECTIVE),
        Word("tired", WordType.ADJECTIVE),
        Word("hungry", WordType.ADJECTIVE),
        Word("thirsty", WordType.ADJECTIVE),
        Word("angry", WordType.ADJECTIVE),
        Word("scared", WordType.ADJECTIVE),
        Word("proud", WordType.ADJECTIVE),
        Word("excited", WordType.ADJECTIVE),
        Word("bored", WordType.ADJECTIVE),
        Word("lazy", WordType.ADJECTIVE),
        Word("tall", WordType.ADJECTIVE),
        Word("healthy", WordType.ADJECTIVE),
        Word("fit", WordType.ADJECTIVE),
        Word("brave", WordType.ADJECTIVE),
        Word("shy", WordType.ADJECTIVE),
        Word("crazy", WordType.ADJECTIVE),
        Word("fancy", WordType.ADJECTIVE),
        Word("messy", WordType.ADJECTIVE),
        Word("tidy", WordType.ADJECTIVE),
        Word("noisy", WordType.ADJECTIVE),
        Word("windy", WordType.ADJECTIVE),
        Word("quiet", WordType.ADJECTIVE),
        Word("fast", WordType.ADJECTIVE),
        Word("fair", WordType.ADJECTIVE),
        Word("wise", WordType.ADJECTIVE),
        Word("tiny", WordType.ADJECTIVE),
        Word("giant", WordType.ADJECTIVE),
        Word("curly", WordType.ADJECTIVE),
        Word("straight", WordType.ADJECTIVE),
        Word("round", WordType.ADJECTIVE),
        Word("square", WordType.ADJECTIVE),
        Word("empty", WordType.ADJECTIVE),
        Word("crowded", WordType.ADJECTIVE),
        Word("narrow", WordType.ADJECTIVE),
        Word("broad", WordType.ADJECTIVE),
        Word("shallow", WordType.ADJECTIVE),
        Word("steep", WordType.ADJECTIVE),
        Word("slippery", WordType.ADJECTIVE),
        Word("rough", WordType.ADJECTIVE),
        Word("tough", WordType.ADJECTIVE),
        Word("ancient", WordType.ADJECTIVE),
        Word("modern", WordType.ADJECTIVE),
        Word("ripe", WordType.ADJECTIVE),
        Word("raw", WordType.ADJECTIVE),
        Word("whole", WordType.ADJECTIVE),
        Word("sole", WordType.ADJECTIVE),
        Word("fake", WordType.ADJECTIVE),
        Word("genuine", WordType.ADJECTIVE),
        Word("dire", WordType.ADJECTIVE),
        Word("keen", WordType.ADJECTIVE),
        Word("grand", WordType.ADJECTIVE),
        Word("plain", WordType.ADJECTIVE),
        Word("spare", WordType.ADJECTIVE),
        Word("bare", WordType.ADJECTIVE),
        Word("nude", WordType.ADJECTIVE),
        Word("brief", WordType.ADJECTIVE),
        Word("crisp", WordType.ADJECTIVE),
        Word("blunt", WordType.ADJECTIVE),
        Word("odd", WordType.ADJECTIVE),
        Word("prime", WordType.ADJECTIVE),
        Word("sheer", WordType.ADJECTIVE),


        // Adjectives (200)
        Word("very", WordType.ADVERB),
        Word("also", WordType.ADVERB),
        Word("however", WordType.ADVERB),
        Word("much", WordType.ADVERB),
        Word("often", WordType.ADVERB),
        Word("almost", WordType.ADVERB),
        Word("always", WordType.ADVERB),
        Word("never", WordType.ADVERB),
        Word("really", WordType.ADVERB),
        Word("simply", WordType.ADVERB),
        Word("quickly", WordType.ADVERB),
        Word("slowly", WordType.ADVERB),
        Word("carefully", WordType.ADVERB),
        Word("easily", WordType.ADVERB),
        Word("loudly", WordType.ADVERB),
        Word("quietly", WordType.ADVERB),
        Word("well", WordType.ADVERB),
        Word("badly", WordType.ADVERB),
        Word("fast", WordType.ADVERB),
        Word("hard", WordType.ADVERB),
        Word("late", WordType.ADVERB),
        Word("early", WordType.ADVERB),
        Word("suddenly", WordType.ADVERB),
        Word("eventually", WordType.ADVERB),
        Word("recently", WordType.ADVERB),
        Word("finally", WordType.ADVERB),
        Word("definitely", WordType.ADVERB),
        Word("extremely", WordType.ADVERB),
        Word("hardly", WordType.ADVERB),
        Word("nearly", WordType.ADVERB),
        Word("rather", WordType.ADVERB),
        Word("somewhat", WordType.ADVERB),
        Word("highly", WordType.ADVERB),
        Word("quite", WordType.ADVERB),
        Word("too", WordType.ADVERB),
        Word("almost", WordType.ADVERB),
        Word("enough", WordType.ADVERB),
        Word("especially", WordType.ADVERB),
        Word("exactly", WordType.ADVERB),
        Word("frequently", WordType.ADVERB),
        Word("gradually", WordType.ADVERB),
        Word("nicely", WordType.ADVERB),
        Word("significantly", WordType.ADVERB),
        Word("sincerely", WordType.ADVERB),
        Word("surely", WordType.ADVERB),
        Word("together", WordType.ADVERB),
        Word("usually", WordType.ADVERB),
        Word("actually", WordType.ADVERB),
        Word("already", WordType.ADVERB),
        Word("clearly", WordType.ADVERB),
        Word("completely", WordType.ADVERB),
        Word("probably", WordType.ADVERB),
        Word("immediately", WordType.ADVERB),
        Word("possibly", WordType.ADVERB),
        Word("currently", WordType.ADVERB),
        Word("formerly", WordType.ADVERB),
        Word("generally", WordType.ADVERB),
        Word("mainly", WordType.ADVERB),
        Word("mostly", WordType.ADVERB),
        Word("namely", WordType.ADVERB),
        Word("naturally", WordType.ADVERB),
        Word("normally", WordType.ADVERB),
        Word("particularly", WordType.ADVERB),
        Word("partly", WordType.ADVERB),
        Word("personally", WordType.ADVERB),
        Word("primarily", WordType.ADVERB),
        Word("properly", WordType.ADVERB),
        Word("readily", WordType.ADVERB),
        Word("certainly", WordType.ADVERB),
        Word("directly", WordType.ADVERB),
        Word("eagerly", WordType.ADVERB),
        Word("effectively", WordType.ADVERB),
        Word("equally", WordType.ADVERB),
        Word("fairly", WordType.ADVERB),
        Word("greatly", WordType.ADVERB),
        Word("hopefully", WordType.ADVERB),
        Word("obviously", WordType.ADVERB),
        Word("perfectly", WordType.ADVERB),
        Word("precisely", WordType.ADVERB),
        Word("promptly", WordType.ADVERB),
        Word("literally", WordType.ADVERB),
        Word("occasionally", WordType.ADVERB),
        Word("rarely", WordType.ADVERB),
        Word("regularly", WordType.ADVERB),
        Word("repeatedly", WordType.ADVERB),
        Word("respectively", WordType.ADVERB),
        Word("seriously", WordType.ADVERB),
        Word("slightly", WordType.ADVERB),
        Word("successfully", WordType.ADVERB),
        Word("unfortunately", WordType.ADVERB),
        Word("widely", WordType.ADVERB),
        Word("wonderfully", WordType.ADVERB),
        Word("yearly", WordType.ADVERB),
        Word("daily", WordType.ADVERB),
        Word("weekly", WordType.ADVERB),
        Word("monthly", WordType.ADVERB),
        Word("below", WordType.PREPOSITION),
        Word("beside", WordType.PREPOSITION),
        Word("towards", WordType.PREPOSITION),
        Word("upon", WordType.PREPOSITION),
        Word("within", WordType.PREPOSITION),
        Word("along", WordType.PREPOSITION),
        Word("until", WordType.PREPOSITION),
        Word("inside", WordType.PREPOSITION),
        Word("outside", WordType.PREPOSITION),
        Word("since", WordType.PREPOSITION),
        Word("despite", WordType.PREPOSITION),
        Word("onto", WordType.PREPOSITION),
        Word("off", WordType.PREPOSITION),
        Word("per", WordType.PREPOSITION),
        Word("up", WordType.PREPOSITION),
        Word("down", WordType.PREPOSITION),
        Word("throughout", WordType.PREPOSITION),
        Word("except", WordType.PREPOSITION),
        Word("versus", WordType.PREPOSITION),

        // Conjunctions (30)
        Word("and", WordType.CONJUNCTION),
        Word("but", WordType.CONJUNCTION),
        Word("or", WordType.CONJUNCTION),
        Word("yet", WordType.CONJUNCTION),
        Word("for", WordType.CONJUNCTION),
        Word("nor", WordType.CONJUNCTION),
        Word("so", WordType.CONJUNCTION),
        Word("because", WordType.CONJUNCTION),
        Word("if", WordType.CONJUNCTION),
        Word("although", WordType.CONJUNCTION),
        Word("since", WordType.CONJUNCTION),
        Word("unless", WordType.CONJUNCTION),
        Word("while", WordType.CONJUNCTION),
        Word("where", WordType.CONJUNCTION),
        Word("after", WordType.CONJUNCTION),
        Word("before", WordType.CONJUNCTION),
        Word("as", WordType.CONJUNCTION),
        Word("than", WordType.CONJUNCTION),
        Word("whether", WordType.CONJUNCTION),
        Word("though", WordType.CONJUNCTION),
        Word("even", WordType.CONJUNCTION),
        Word("once", WordType.CONJUNCTION),
        Word("until", WordType.CONJUNCTION),
        Word("when", WordType.CONJUNCTION),
        Word("whenever", WordType.CONJUNCTION),
        Word("whereas", WordType.CONJUNCTION),
        Word("wherever", WordType.CONJUNCTION),
        Word("while", WordType.CONJUNCTION),
        Word("otherwise", WordType.CONJUNCTION),
        Word("however", WordType.CONJUNCTION),

        // Pronouns (30)
        Word("I", WordType.PRONOUN),
        Word("you", WordType.PRONOUN),
        Word("he", WordType.PRONOUN),
        Word("she", WordType.PRONOUN),
        Word("it", WordType.PRONOUN),
        Word("we", WordType.PRONOUN),
        Word("they", WordType.PRONOUN),
        Word("me", WordType.PRONOUN),
        Word("him", WordType.PRONOUN),
        Word("her", WordType.PRONOUN),
        Word("us", WordType.PRONOUN),
        Word("them", WordType.PRONOUN),
        Word("who", WordType.PRONOUN),
        Word("whom", WordType.PRONOUN),
        Word("whose", WordType.PRONOUN),
        Word("which", WordType.PRONOUN),
        Word("that", WordType.PRONOUN),
        Word("myself", WordType.PRONOUN),
        Word("yourself", WordType.PRONOUN),
        Word("himself", WordType.PRONOUN),
        Word("herself", WordType.PRONOUN),
        Word("itself", WordType.PRONOUN),
        Word("ourselves", WordType.PRONOUN),
        Word("themselves", WordType.PRONOUN),
        Word("each", WordType.PRONOUN),
        Word("few", WordType.PRONOUN),
        Word("many", WordType.PRONOUN),
        Word("several", WordType.PRONOUN),
        Word("all", WordType.PRONOUN),
        Word("any", WordType.PRONOUN),

        // Conjunctions (30)
        Word("and", WordType.CONJUNCTION),
        Word("but", WordType.CONJUNCTION),
        Word("or", WordType.CONJUNCTION),
        Word("yet", WordType.CONJUNCTION),
        Word("for", WordType.CONJUNCTION),
        Word("nor", WordType.CONJUNCTION),
        Word("so", WordType.CONJUNCTION),
        Word("because", WordType.CONJUNCTION),
        Word("if", WordType.CONJUNCTION),
        Word("although", WordType.CONJUNCTION),
        Word("since", WordType.CONJUNCTION),
        Word("unless", WordType.CONJUNCTION),
        Word("while", WordType.CONJUNCTION),
        Word("where", WordType.CONJUNCTION),
        Word("after", WordType.CONJUNCTION),
        Word("before", WordType.CONJUNCTION),
        Word("as", WordType.CONJUNCTION),
        Word("than", WordType.CONJUNCTION),
        Word("whether", WordType.CONJUNCTION),
        Word("though", WordType.CONJUNCTION),
        Word("even", WordType.CONJUNCTION),
        Word("once", WordType.CONJUNCTION),
        Word("until", WordType.CONJUNCTION),
        Word("when", WordType.CONJUNCTION),
        Word("whenever", WordType.CONJUNCTION),
        Word("whereas", WordType.CONJUNCTION),
        Word("wherever", WordType.CONJUNCTION),
        Word("while", WordType.CONJUNCTION),
        Word("otherwise", WordType.CONJUNCTION),
        Word("however", WordType.CONJUNCTION),

        // Pronouns (30)
        Word("I", WordType.PRONOUN),
        Word("you", WordType.PRONOUN),
        Word("he", WordType.PRONOUN),
        Word("she", WordType.PRONOUN),
        Word("it", WordType.PRONOUN),
        Word("we", WordType.PRONOUN),
        Word("they", WordType.PRONOUN),
        Word("me", WordType.PRONOUN),
        Word("him", WordType.PRONOUN),
        Word("her", WordType.PRONOUN),
        Word("us", WordType.PRONOUN),
        Word("them", WordType.PRONOUN),
        Word("who", WordType.PRONOUN),
        Word("whom", WordType.PRONOUN),
        Word("whose", WordType.PRONOUN),
        Word("which", WordType.PRONOUN),
        Word("that", WordType.PRONOUN),
        Word("myself", WordType.PRONOUN),
        Word("yourself", WordType.PRONOUN),
        Word("himself", WordType.PRONOUN),
        Word("herself", WordType.PRONOUN),
        Word("itself", WordType.PRONOUN),
        Word("ourselves", WordType.PRONOUN),
        Word("themselves", WordType.PRONOUN),
        Word("each", WordType.PRONOUN),
        Word("few", WordType.PRONOUN),
        Word("many", WordType.PRONOUN),
        Word("several", WordType.PRONOUN),
        Word("all", WordType.PRONOUN),
        Word("any", WordType.PRONOUN),

        // Determiners (20)
        Word("the", WordType.DETERMINER),
        Word("a", WordType.DETERMINER),
        Word("an", WordType.DETERMINER),
        Word("this", WordType.DETERMINER),
        Word("that", WordType.DETERMINER),
        Word("these", WordType.DETERMINER),
        Word("those", WordType.DETERMINER),
        Word("livvy dunne", WordType.DETERMINER),
        Word("my", WordType.DETERMINER),
        Word("your", WordType.DETERMINER),
        Word("his", WordType.DETERMINER),
        Word("her", WordType.DETERMINER),
        Word("its", WordType.DETERMINER),
        Word("our", WordType.DETERMINER),
        Word("their", WordType.DETERMINER),
        Word("some", WordType.DETERMINER),
        Word("any", WordType.DETERMINER),
        Word("every", WordType.DETERMINER),
        Word("no", WordType.DETERMINER),
        Word("each", WordType.DETERMINER),
        Word("either", WordType.DETERMINER)
    )
    private val irregularVerbs = mapOf(
        "be" to mapOf("present" to "is", "past" to "was", "pastParticiple" to "been"),
        "have" to mapOf("present" to "has", "past" to "had", "pastParticiple" to "had"),
        "do" to mapOf("present" to "does", "past" to "did", "pastParticiple" to "done"),
        "go" to mapOf("present" to "goes", "past" to "went", "pastParticiple" to "gone"),
        "say" to mapOf("present" to "says", "past" to "said", "pastParticiple" to "said"),
        "eat" to mapOf("present" to "eats", "past" to "ate", "pastParticiple" to "eaten"),
        "see" to mapOf("present" to "sees", "past" to "saw", "pastParticiple" to "seen"),
        "give" to mapOf("present" to "gives", "past" to "gave", "pastParticiple" to "given"),
        "take" to mapOf("present" to "takes", "past" to "took", "pastParticiple" to "taken"),
        "make" to mapOf("present" to "makes", "past" to "made", "pastParticiple" to "made")
    )

    val dramaticAdjectives = listOf(
        "haunting",
        "mysterious",
        "breathtaking",
        "intense",
        "tragic",
        "triumphant",
        "devastating",
        "awe-inspiring",
        "heart-wrenching",
        "electrifying",
        "mesmerizing",
        "shocking",
        "gripping",
        "suspenseful",
        "chilling",
        "passionate",
        "earth-shattering",
        "mind-blowing",
        "spine-tingling",
        "heart-stopping",
        "soul-crushing",
        "life-changing",
        "overwhelming",
        "extraordinary",
        "monumental",
        "unfathomable",
        "unimaginable",
        "unprecedented",
        "revolutionary",
        "cataclysmic",
        "apocalyptic",
        "epic",
        "legendary",
        "mythical",
        "otherworldly",
        "surreal",
        "ethereal",
        "transcendent",
        "transformative",
        "pivotal",
        "climactic",
        "fateful",
        "momentous",
        "profound",
        "staggering",
        "astounding",
        "astonishing",
        "jaw-dropping",
        "spectacular",
        "phenomenal",
        "inconceivable",
        "unbelievable",
        "incomprehensible",
        "enigmatic",
        "perplexing",
        "baffling",
        "bewildering",
        "mystifying",
        "spellbinding",
        "enthralling",
        "captivating",
        "enchanting",
        "bewitching",
        "hypnotic",
        "trance-inducing",
        "entrancing",
        "riveting",
        "compelling",
        "engrossing",
        "all-consuming",
        "obsessive",
        "addictive",
        "intoxicating",
        "exhilarating",
        "thrilling",
        "pulse-pounding",
        "adrenaline-pumping",
        "nerve-wracking",
        "nail-biting",
        "hair-raising",
        "blood-curdling",
        "bone-chilling",
        "petrifying",
        "terrifying",
        "horrifying",
        "nightmarish",
        "hellish",
        "infernal",
        "demonic",
        "angelic",
        "celestial",
        "divine",
        "miraculous",
        "wondrous",
        "marvelous",
        "prodigious",
        "phenomenal",
        "colossal",
        "titanic",
        "gargantuan",
        "behemoth",
        "leviathan",
        "cosmic",
        "universal",
        "infinite",
        "eternal",
        "timeless",
        "ageless",
        "primordial",
        "ancient",
        "primeval",
        "prehistoric",
        "antediluvian",
        "forgotten",
        "lost",
        "hidden",
        "veiled",
        "shrouded",
        "clandestine",
        "surreptitious",
        "clandestine",
        "furtive",
        "sly",
        "cunning",
        "devious",
        "treacherous",
        "perilous",
        "hazardous",
        "precarious",
        "volatile",
        "unstable",
        "tumultuous",
        "turbulent",
        "tempestuous",
        "stormy",
        "thunderous",
        "deafening",
        "cacophonous",
        "ear-splitting",
        "piercing",
        "shrill",
        "strident",
        "discordant",
        "jarring",
        "grating",
        "abrasive",
        "caustic",
        "acerbic",
        "vitriolic",
        "venomous",
        "toxic",
        "noxious",
        "pernicious",
        "malevolent",
        "sinister",
        "diabolical",
        "fiendish",
        "wicked",
        "evil",
        "malicious",
        "spiteful",
        "vindictive",
        "ruthless",
        "merciless",
        "pitiless",
        "remorseless",
        "unforgiving",
        "implacable",
        "relentless",
        "inexorable",
        "indomitable",
        "invincible",
        "unconquerable",
        "unassailable",
        "impregnable",
        "indestructible",
        "eternal",
        "everlasting",
        "perpetual",
        "immortal",
        "undying",
        "deathless",
        "imperishable",
        "incorruptible",
        "inviolable",
        "sacrosanct",
        "hallowed",
        "revered",
        "venerated",
        "exalted",
        "glorified",
        "magnificent",
        "splendid",
        "resplendent",
        "radiant",
        "luminous",
        "incandescent",
        "effulgent",
        "dazzling",
        "blinding",
        "searing",
        "scorching",
        "blistering",
        "sweltering",
        "suffocating",
        "stifling",
        "oppressive",
        "overwhelming",
        "crushing",
        "pulverizing",
        "annihilating",
        "obliterating",
        "eradicating",
        "exterminating",
        "decimating",
        "ravaging",
        "devastating",
        "cataclysmic",
        "apocalyptic"
    )

    val dramaticVerbs = listOf(
        "shatter",
        "illuminate",
        "ignite",
        "consume",
        "unravel",
        "transform",
        "devastate",
        "captivate",
        "unleash",
        "conquer",
        "betray",
        "sacrifice",
        "defy",
        "annihilate",
        "obliterate",
        "eradicate",
        "decimate",
        "ravage",
        "demolish",
        "pulverize",
        "incinerate",
        "vaporize",
        "disintegrate",
        "eviscerate",
        "excoriate",
        "exterminate",
        "extirpate",
        "annul",
        "nullify",
        "negate",
        "invalidate",
        "counteract",
        "neutralize",
        "counterbalance",
        "offset",
        "eclipse",
        "overshadow",
        "outshine",
        "surpass",
        "transcend",
        "exceed",
        "excel",
        "outdo",
        "outperform",
        "outclass",
        "outstrip",
        "overwhelm",
        "overpower",
        "overawe",
        "intimidate",
        "daunt",
        "unnerve",
        "disconcert",
        "fluster",
        "rattle",
        "shake",
        "jar",
        "jolt",
        "shock",
        "stun",
        "stupefy",
        "dumbfound",
        "flabbergast",
        "astound",
        "astonish",
        "amaze",
        "startle",
        "surprise",
        "alarm",
        "frighten",
        "terrify",
        "horrify",
        "petrify",
        "paralyze",
        "immobilize",
        "transfix",
        "mesmerize",
        "hypnotize",
        "entrance",
        "enchant",
        "bewitch",
        "spellbind",
        "enthrall",
        "captivate",
        "fascinate",
        "intrigue",
        "tantalize",
        "allure",
        "entice",
        "lure",
        "seduce",
        "beguile",
        "charm",
        "enrapture",
        "delight",
        "thrill",
        "excite",
        "elate",
        "exhilarate",
        "intoxicate",
        "inebriate",
        "exalt",
        "uplift",
        "elevate",
        "inspire",
        "motivate",
        "galvanize",
        "energize",
        "invigorate",
        "revitalize",
        "rejuvenate",
        "regenerate",
        "resurrect",
        "revive",
        "rekindle",
        "reignite",
        "reawaken",
        "renew",
        "restore",
        "reclaim",
        "redeem",
        "salvage",
        "rescue",
        "liberate",
        "emancipate",
        "unshackle",
        "unfetter",
        "unleash",
        "unchain",
        "unbound",
        "free",
        "release",
        "discharge",
        "emit",
        "exude",
        "radiate",
        "emanate",
        "disseminate",
        "propagate",
        "proliferate",
        "multiply",
        "burgeon",
        "flourish",
        "thrive",
        "prosper",
        "succeed",
        "triumph",
        "prevail",
        "vanquish",
        "subjugate",
        "dominate",
        "tyrannize",
        "oppress",
        "suppress",
        "repress",
        "quell",
        "quash",
        "squelch",
        "stifle",
        "smother",
        "suffocate",
        "choke",
        "strangle",
        "throttle",
        "constrict",
        "compress",
        "crush",
        "squash",
        "mangle",
        "mutilate",
        "butcher",
        "slaughter",
        "massacre",
        "decimate",
        "annihilate"
    )

    val dramaticAdverbs = listOf(
        "dramatically",
        "intensely",
        "passionately",
        "desperately",
        "relentlessly",
        "feverishly",
        "breathlessly",
        "hauntingly",
        "tragically",
        "fiercely",
        "violently",
        "savagely",
        "brutally",
        "mercilessly",
        "ruthlessly",
        "viciously",
        "ferociously",
        "voraciously",
        "ravenously",
        "insatiably",
        "uncontrollably",
        "irresistibly",
        "compulsively",
        "obsessively",
        "maniacally",
        "fanatically",
        "zealously",
        "fervently",
        "ardently",
        "vehemently",
        "vigorously",
        "forcefully",
        "powerfully",
        "mightily",
        "potently",
        "overwhelmingly",
        "staggeringly",
        "shockingly",
        "alarmingly",
        "disturbingly",
        "disconcertingly",
        "unsettlingly",
        "unnerving",
        "chilling",
        "horrifying",
        "terrifying",
        "petrifying",
        "paralyzing",
        "stupefying",
        "bewildering",
        "baffling",
        "perplexing",
        "mystifying",
        "enigmatically",
        "cryptically",
        "inscrutably",
        "unfathomably",
        "incomprehensibly",
        "inconceivably",
        "unimaginably",
        "unbelievably",
        "incredibly",
        "astonishingly",
        "amazingly",
        "stunningly",
        "spectacularly",
        "phenomenally",
        "extraordinarily",
        "remarkably",
        "exceptionally",
        "outstandingly",
        "supremely",
        "sublimely",
        "transcendently",
        "divinely",
        "celestially",
        "ethereally",
        "surreally",
        "phantasmagorically",
        "bizarrely",
        "grotesquely",
        "macabrely",
        "ghoulishly",
        "diabolically",
        "infernally",
        "hellishly",
        "demonically",
        "fiendishly",
        "malevolently",
        "maliciously",
        "viciously",
        "venomously",
        "virulently",
        "caustically",
        "acidly",
        "bitingly",
        "scathingly",
        "witheringly",
        "devastatingly",
        "crushingly",
        "overwhelmingly",
        "thunderously",
        "deafeningly",
        "ear-splittingly",
        "piercingly",
        "shrilly",
        "stridently",
        "cacophonously",
        "discordantly",
        "jarringly",
        "gratingly",
        "abrasively",
        "harshly",
        "roughly",
        "violently",
        "turbulently",
        "tumultuously",
        "chaotically",
        "frenetically",
        "frantically",
        "manically",
        "wildly",
        "furiously",
        "tempestuously",
        "stormily",
        "thunderously",
        "explosively",
        "cataclysmic",
        "apocalyptically",
        "earth-shatteringly",
        "world-endingly",
        "cosmic",
        "universally",
        "eternally",
        "perpetually",
        "endlessly",
        "infinitely",
        "boundlessly",
        "limitlessly",
        "vastly",
        "immensely",
        "enormously",
        "tremendously",
        "hugely",
        "massively",
        "colossally",
        "titanically",
        "gigantically",
        "monumentally",
        "epically",
        "legendarily",
        "mythically",
        "immortally",
        "timelessly",
        "agelessly",
        "primordially",
        "anciently",
        "prehistorically",
        "antediluvially"
    )

    private val sentenceStructures = listOf(
        listOf(WordType.DETERMINER, WordType.NOUN, WordType.VERB),
        listOf(WordType.DETERMINER, WordType.ADJECTIVE, WordType.NOUN, WordType.VERB),
        listOf(WordType.PRONOUN, WordType.VERB, WordType.ADVERB),
        listOf(
            WordType.DETERMINER, WordType.NOUN, WordType.VERB, WordType.PREPOSITION, WordType.DETERMINER, WordType.NOUN
        ),
        listOf(WordType.ADJECTIVE, WordType.NOUN, WordType.VERB, WordType.ADVERB),
        listOf(WordType.PRONOUN, WordType.VERB, WordType.DETERMINER, WordType.NOUN),
        listOf(WordType.ADVERB, WordType.ADJECTIVE, WordType.NOUN, WordType.VERB),
        listOf(WordType.DETERMINER, WordType.NOUN, WordType.VERB, WordType.ADVERB),
        listOf(
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.VERB,
            WordType.PREPOSITION,
            WordType.DETERMINER,
            WordType.NOUN
        ),
        listOf(WordType.PRONOUN, WordType.VERB, WordType.DETERMINER, WordType.ADJECTIVE, WordType.NOUN),
        listOf(
            WordType.ADVERB,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.VERB,
            WordType.PREPOSITION,
            WordType.DETERMINER,
            WordType.NOUN
        ),
        listOf(
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.VERB,
            WordType.CONJUNCTION,
            WordType.PRONOUN,
            WordType.VERB,
            WordType.ADVERB
        ),
        listOf(
            WordType.PRONOUN,
            WordType.ADVERB,
            WordType.VERB,
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.PREPOSITION,
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN
        ),
        listOf(WordType.NOUN, WordType.VERB, WordType.PREPOSITION, WordType.NOUN),
        listOf(WordType.DETERMINER, WordType.NOUN, WordType.VERB, WordType.ADJECTIVE),
        listOf(WordType.PRONOUN, WordType.ADVERB, WordType.VERB, WordType.DETERMINER, WordType.NOUN),
        listOf(
            WordType.ADVERB,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.VERB,
            WordType.CONJUNCTION,
            WordType.VERB,
            WordType.DETERMINER,
            WordType.NOUN
        ),
        listOf(
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.VERB,
            WordType.PREPOSITION,
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.DETERMINER,
            WordType.NOUN
        ),
        listOf(
            WordType.PRONOUN,
            WordType.VERB,
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.PREPOSITION,
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN
        ),
        listOf(
            WordType.ADVERB,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.VERB,
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.PREPOSITION,
            WordType.DETERMINER,
            WordType.NOUN
        ),
        listOf(
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.VERB,
            WordType.ADVERB,
            WordType.CONJUNCTION,
            WordType.ADVERB
        ),
        listOf(
            WordType.PRONOUN,
            WordType.VERB,
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN
        ),
        listOf(
            WordType.ADVERB,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.VERB,
            WordType.PREPOSITION,
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.DETERMINER,
            WordType.NOUN
        ),
        listOf(
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.VERB,
            WordType.ADJECTIVE,
            WordType.CONJUNCTION,
            WordType.ADJECTIVE
        ),
        listOf(
            WordType.PRONOUN,
            WordType.ADVERB,
            WordType.VERB,
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.PREPOSITION,
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.NOUN
        ),
        listOf(
            WordType.ADVERB,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.VERB,
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.PREPOSITION,
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.NOUN
        ),
        listOf(
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.VERB,
            WordType.ADVERB,
            WordType.CONJUNCTION,
            WordType.ADVERB,
            WordType.CONJUNCTION,
            WordType.ADVERB
        ),
        listOf(
            WordType.PRONOUN,
            WordType.VERB,
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.PREPOSITION,
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.NOUN
        ),
        listOf(
            WordType.ADVERB,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.VERB,
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.PREPOSITION,
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.NOUN
        ),
        listOf(
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.VERB,
            WordType.ADVERB,
            WordType.CONJUNCTION,
            WordType.ADVERB,
            WordType.CONJUNCTION,
            WordType.ADVERB,
            WordType.CONJUNCTION,
            WordType.ADVERB
        ),
        listOf(
            WordType.PRONOUN,
            WordType.VERB,
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.PREPOSITION,
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.NOUN
        ),
        listOf(
            WordType.ADVERB,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.VERB,
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.PREPOSITION,
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.NOUN
        ),
        listOf(
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.VERB,
            WordType.ADVERB,
            WordType.CONJUNCTION,
            WordType.ADVERB,
            WordType.CONJUNCTION,
            WordType.ADVERB,
            WordType.CONJUNCTION,
            WordType.ADVERB,
            WordType.CONJUNCTION,
            WordType.ADVERB
        ),
        listOf(
            WordType.PRONOUN,
            WordType.VERB,
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.PREPOSITION,
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.NOUN
        ),
        listOf(
            WordType.ADVERB,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.VERB,
            WordType.DETERMINER,
            WordType.NOUN,
            WordType.PREPOSITION,
            WordType.DETERMINER,
            WordType.ADJECTIVE,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.NOUN,
            WordType.CONJUNCTION,
            WordType.NOUN
        ),
        listOf(
            WordType.DETERMINER, WordType.ADJECTIVE, WordType.NOUN, WordType.VERB
        )
    )

    private val pluralNouns = setOf(
        "people", "children", "men", "women", "feet", "teeth", "mice", "geese",
        "lives", "knives", "wives", "elves", "halves", "leaves", "loaves", "potatoes", "tomatoes", "cacti",
        "foci", "fungi", "nuclei", "syllabi", "radii", "stimuli", "alumni", "criteria", "phenomena",
        "data", "media", "analyses", "diagnoses", "theses", "crises", "oases", "paralyses", "hypotheses",
        "synopses", "appendices", "matrices", "vertices", "axes", "bases", "crises", "theses", "theses",
        "phenomena", "criteria", "bacteria", "amoebae", "antennae", "antitheses", "axes", "bacilli",
        "bureaux", "cacti", "corpora", "curricula", "curves", "dicta", "effluvia", "ellipses", "errata",
        "faux pas", "foci", "foci", "formulae", "fungi", "genera", "hypotheses", "indices", "larvae",
        "libretti", "matrices", "memoranda", "minutiae", "nebulae", "nuclei", "oases", "parentheses",
        "phenomena", "phyla", "platypi", "prognoses", "quanta", "rostra", "scholia", "soliloquies",
    )


    private val teenSlang = listOf(
        "lit",
        "fire",
        "cap",
        "no cap",
        "bet",
        "salty",
        "sus",
        "yeet",
        "flex",
        "slay",
        "bussin",
        "sheesh",
        "goated",
        "based",
        "mid",
        "slaps",
        "finna",
        "lowkey",
        "highkey",
        "vibe",
        "bet",
        "fr",
        "deadass",
        "on god",
        "simp",
        "stan",
        "tea",
        "shook",
        "extra"
    )


    private val sentenceTypes = listOf(
        "normal", "dramatic", "funny", "teenADHD", "poetic", "scientific", "philosophical", "sarcastic",
        "noir", "fantasy", "cyberpunk", "historical", "romantic", "horror", "mystery", "western"
    )

    private fun generateStructureForType(type: String): List<WordType> {
        return when (type) {
            "normal" -> generateSentenceStructure()
            "dramatic" -> generateDramaticStructure()
            "funny" -> generateFunnyStructure()
            "teenADHD" -> generateTeenADHDStructure()
            "poetic" -> generatePoeticStructure()
            "scientific" -> generateScientificStructure()
            "philosophical" -> generatePhilosophicalStructure()
            "sarcastic" -> generateSarcasticStructure()
            "noir" -> generateNoirStructure()
            "fantasy" -> generateFantasyStructure()
            "cyberpunk" -> generateCyberpunkStructure()
            "historical" -> generateHistoricalStructure()
            "romantic" -> generateRomanticStructure()
            "horror" -> generateHorrorStructure()
            "mystery" -> generateMysteryStructure()
            "western" -> generateWesternStructure()
            else -> generateSentenceStructure()
        }
    }

    private fun generateHistoricalStructure(): List<WordType> {
        val baseStructures = listOf(
            listOf(
                WordType.DETERMINER,
                WordType.ADJECTIVE,
                WordType.HISTORICAL_NOUN,
                WordType.HISTORICAL_VERB,
                WordType.ADVERB
            ),
            listOf(
                WordType.HISTORICAL_FIGURE,
                WordType.HISTORICAL_VERB,
                WordType.DETERMINER,
                WordType.ADJECTIVE,
                WordType.HISTORICAL_NOUN
            ),
            listOf(
                WordType.PREPOSITION,
                WordType.DETERMINER,
                WordType.HISTORICAL_ERA,
                WordType.COMMA,
                WordType.HISTORICAL_NOUN,
                WordType.HISTORICAL_VERB
            ),
            listOf(
                WordType.HISTORICAL_LOCATION,
                WordType.VERB,
                WordType.DETERMINER,
                WordType.ADJECTIVE,
                WordType.HISTORICAL_NOUN
            )
        )
        return baseStructures.random() + listOf(WordType.HISTORICAL_PHRASE)
    }

    private fun generateRomanticStructure(): List<WordType> {
        val baseStructures = listOf(
            listOf(
                WordType.DETERMINER,
                WordType.ADJECTIVE,
                WordType.ROMANTIC_NOUN,
                WordType.ROMANTIC_VERB,
                WordType.ADVERB
            ),
            listOf(
                WordType.PRONOUN,
                WordType.ROMANTIC_VERB,
                WordType.POSSESSIVE,
                WordType.ADJECTIVE,
                WordType.ROMANTIC_NOUN
            ),
            listOf(
                WordType.PREPOSITION,
                WordType.DETERMINER,
                WordType.ROMANTIC_LOCATION,
                WordType.COMMA,
                WordType.PRONOUN,
                WordType.ROMANTIC_VERB
            ),
            listOf(
                WordType.ROMANTIC_ADJECTIVE,
                WordType.ROMANTIC_NOUN,
                WordType.VERB,
                WordType.PREPOSITION,
                WordType.POSSESSIVE,
                WordType.ROMANTIC_BODY_PART
            )
        )
        return baseStructures.random() + listOf(WordType.ROMANTIC_PHRASE)
    }


    private fun generateHorrorStructure(): List<WordType> {
        val baseStructures = listOf(
            listOf(
                WordType.DETERMINER,
                WordType.ADJECTIVE,
                WordType.HORROR_NOUN,
                WordType.HORROR_VERB,
                WordType.ADVERB
            ),
            listOf(
                WordType.PREPOSITION,
                WordType.DETERMINER,
                WordType.HORROR_LOCATION,
                WordType.COMMA,
                WordType.HORROR_CREATURE,
                WordType.HORROR_VERB
            ),
            listOf(
                WordType.HORROR_SOUND,
                WordType.VERB,
                WordType.PREPOSITION,
                WordType.DETERMINER,
                WordType.ADJECTIVE,
                WordType.HORROR_NOUN
            ),
            listOf(
                WordType.PRONOUN,
                WordType.HORROR_VERB,
                WordType.DETERMINER,
                WordType.HORROR_BODY_PART,
                WordType.HORROR_DESCRIPTION
            )
        )
        return baseStructures.random() + listOf(WordType.HORROR_PHRASE)
    }

    private fun generateMysteryStructure(): List<WordType> {
        val baseStructures = listOf(
            listOf(
                WordType.DETERMINER,
                WordType.ADJECTIVE,
                WordType.MYSTERY_NOUN,
                WordType.MYSTERY_VERB,
                WordType.ADVERB
            ),
            listOf(
                WordType.MYSTERY_DETECTIVE,
                WordType.MYSTERY_VERB,
                WordType.DETERMINER,
                WordType.ADJECTIVE,
                WordType.MYSTERY_CLUE
            ),
            listOf(
                WordType.PREPOSITION,
                WordType.DETERMINER,
                WordType.MYSTERY_LOCATION,
                WordType.COMMA,
                WordType.PRONOUN,
                WordType.MYSTERY_VERB,
                WordType.DETERMINER,
                WordType.MYSTERY_EVIDENCE
            ),
            listOf(
                WordType.MYSTERY_SUSPECT,
                WordType.VERB,
                WordType.DETERMINER,
                WordType.ADJECTIVE,
                WordType.MYSTERY_ALIBI
            )
        )
        return baseStructures.random() + listOf(WordType.MYSTERY_PHRASE)
    }

    private fun generateWesternStructure(): List<WordType> {
        val baseStructures = listOf(
            listOf(
                WordType.DETERMINER,
                WordType.ADJECTIVE,
                WordType.WESTERN_NOUN,
                WordType.WESTERN_VERB,
                WordType.ADVERB
            ),
            listOf(
                WordType.WESTERN_CHARACTER,
                WordType.WESTERN_VERB,
                WordType.PREPOSITION,
                WordType.DETERMINER,
                WordType.WESTERN_LOCATION
            ),
            listOf(
                WordType.PREPOSITION,
                WordType.DETERMINER,
                WordType.WESTERN_TIME,
                WordType.COMMA,
                WordType.WESTERN_NOUN,
                WordType.WESTERN_VERB
            ),
            listOf(
                WordType.WESTERN_EXCLAMATION,
                WordType.COMMA,
                WordType.PRONOUN,
                WordType.WESTERN_VERB,
                WordType.DETERMINER,
                WordType.WESTERN_ITEM
            )
        )
        return baseStructures.random() + listOf(WordType.WESTERN_PHRASE)
    }

    private fun getHistoricalWord(type: WordType, isPlural: Boolean = false): String {
        val historicalWords = mapOf(
            WordType.ADJECTIVE to listOf(
                "ancient",
                "medieval",
                "colonial",
                "revolutionary",
                "imperial",
                "feudal",
                "prehistoric",
                "Victorian",
                "Elizabethan",
                "Napoleonic"
            ),
            WordType.ADVERB to listOf(
                "historically",
                "traditionally",
                "formerly",
                "anciently",
                "classically",
                "momentously",
                "epochally",
                "consequentially"
            ),
            WordType.HISTORICAL_VERB to listOf(
                "conquer",
                "reign",
                "revolt",
                "colonize",
                "discover",
                "establish",
                "decree",
                "overthrow",
                "unify",
                "chronicle"
            ),
            WordType.HISTORICAL_NOUN to listOf(
                "emperor",
                "kingdom",
                "revolution",
                "dynasty",
                "artifact",
                "decree",
                "conquest",
                "treaty",
                "rebellion",
                "expedition"
            ),
            WordType.HISTORICAL_FIGURE to listOf(
                "Napoleon",
                "Cleopatra",
                "Julius Caesar",
                "Queen Victoria",
                "Genghis Khan",
                "Joan of Arc",
                "Alexander the Great"
            ),
            WordType.HISTORICAL_ERA to listOf(
                "Renaissance",
                "Middle Ages",
                "Industrial Revolution",
                "Age of Enlightenment",
                "Bronze Age",
                "Classical Antiquity"
            ),
            WordType.HISTORICAL_LOCATION to listOf(
                "Ancient Rome",
                "Medieval Europe",
                "Colonial America",
                "Imperial China",
                "Aztec Empire",
                "Ottoman Empire"
            ),
            WordType.HISTORICAL_PHRASE to listOf(
                "echoing through the annals of time",
                "shaping the course of history",
                "leaving an indelible mark on civilization"
            )
        )
        return historicalWords[type]?.random() ?: chooseWord(type, Tense.values().random(), isPlural, null)
    }

    private fun getRomanticWord(type: WordType, isPlural: Boolean = false): String {
        val romanticWords = mapOf(
            WordType.ADJECTIVE to listOf(
                "passionate",
                "tender",
                "ardent",
                "amorous",
                "enchanting",
                "alluring",
                "captivating",
                "enthralling",
                "bewitching",
                "sensual"
            ),
            WordType.ADVERB to listOf(
                "lovingly",
                "tenderly",
                "passionately",
                "adoringly",
                "affectionately",
                "devotedly",
                "ardently",
                "fervently",
                "yearningly"
            ),
            WordType.ROMANTIC_VERB to listOf(
                "adore",
                "cherish",
                "embrace",
                "caress",
                "woo",
                "enchant",
                "captivate",
                "seduce",
                "enthrall",
                "beguile"
            ),
            WordType.ROMANTIC_NOUN to listOf(
                "lover",
                "heart",
                "soulmate",
                "romance",
                "passion",
                "desire",
                "affection",
                "devotion",
                "infatuation",
                "ardor"
            ),
            WordType.ROMANTIC_ADJECTIVE to listOf(
                "lovestruck",
                "starry-eyed",
                "head-over-heels",
                "smitten",
                "enamored",
                "besotted",
                "love-sick",
                "devoted"
            ),
            WordType.ROMANTIC_LOCATION to listOf(
                "moonlit garden",
                "secluded beach",
                "candlelit room",
                "starry night sky",
                "blossoming meadow",
                "cozy fireplace"
            ),
            WordType.ROMANTIC_BODY_PART to listOf("heart", "lips", "eyes", "hands", "arms", "soul"),
            WordType.ROMANTIC_PHRASE to listOf(
                "lost in each other's eyes",
                "swept off their feet",
                "falling head over heels",
                "love at first sight"
            )
        )

        return romanticWords[type]?.randomOrNull()
            ?: chooseWord(type, Tense.values().random(), isPlural, null)
    }

    private fun getHorrorWord(type: WordType, isPlural: Boolean = false): String {
        val horrorWords = mapOf(
            WordType.ADJECTIVE to listOf(
                "eerie",
                "chilling",
                "terrifying",
                "sinister",
                "macabre",
                "ghastly",
                "horrific",
                "blood-curdling",
                "nightmarish",
                "malevolent"
            ),
            WordType.ADVERB to listOf(
                "ominously",
                "eerily",
                "horrifyingly",
                "dreadfully",
                "menacingly",
                "grotesquely",
                "spine-chillingly",
                "hauntingly"
            ),
            WordType.HORROR_VERB to listOf(
                "haunt",
                "terrify",
                "lurk",
                "stalk",
                "devour",
                "possess",
                "mutilate",
                "torment",
                "creep",
                "slither"
            ),
            WordType.HORROR_NOUN to listOf(
                "ghost",
                "monster",
                "nightmare",
                "darkness",
                "scream",
                "apparition",
                "specter",
                "demon",
                "abyss",
                "crypt"
            ),
            WordType.HORROR_CREATURE to listOf(
                "werewolf",
                "vampire",
                "zombie",
                "poltergeist",
                "banshee",
                "wendigo",
                "doppelganger",
                "ghoul"
            ),
            WordType.HORROR_LOCATION to listOf(
                "abandoned mansion",
                "dark forest",
                "forgotten crypt",
                "misty graveyard",
                "cursed castle",
                "haunted asylum"
            ),
            WordType.HORROR_SOUND to listOf(
                "blood-curdling scream",
                "ghostly whisper",
                "bone-chilling howl",
                "ominous creaking",
                "terrifying silence"
            ),
            WordType.HORROR_BODY_PART to listOf(
                "rotting flesh",
                "hollow eyes",
                "twisted limbs",
                "bloody fangs",
                "spectral form"
            ),
            WordType.HORROR_DESCRIPTION to listOf(
                "dripping with viscous slime",
                "emanating an otherworldly glow",
                "pulsating with unholy life",
                "shrouded in darkness"
            ),
            WordType.HORROR_PHRASE to listOf(
                "sending shivers down the spine",
                "freezing the blood in their veins",
                "striking terror into their hearts"
            )
        )
        return horrorWords[type]?.random() ?: chooseWord(type, Tense.values().random(), isPlural, null)
    }

    private fun getMysteryWord(type: WordType, isPlural: Boolean = false): String {
        val mysteryWords = mapOf(
            WordType.ADJECTIVE to listOf(
                "mysterious",
                "enigmatic",
                "perplexing",
                "cryptic",
                "baffling",
                "elusive",
                "intriguing",
                "suspicious",
                "clandestine",
                "covert",
                "enigmatic",
                "inscrutable",
                "puzzling",
                "ambiguous",
                "obscure",
                "arcane",
                "esoteric",
                "abstruse",
                "opaque",
                "cryptic",
                "inexplicable",
                "unfathomable",
                "unexplainable",
                "unaccountable",
                "incomprehensible",
                "unintelligible",
                "unconventional",
                "unorthodox",
                "unusual",
                "uncommon",
                "unprecedented",
                "unheard-of",
                "unseen",
                "unwitnessed",
                "unobserved",
                "unnoticed",
                "unperceived",
                "unappreciated",
                "unacknowledged",
                "unrecognized",
                "unidentified",
                "unmarked",
                "unbranded",
                "unlabeled",
                "unstamped",
                "unsealed",
                "unopened",
                "unbroken",
                "unscathed",
                "unharmed",

                ),
            WordType.ADVERB to listOf(
                "mysteriously",
                "secretly",
                "covertly",
                "suspiciously",
                "enigmatically",
                "surreptitiously",
                "stealthily",
                "furtively",
                "clandestinely",
                "conspiratorially",
                "cryptically"
            ),
            WordType.MYSTERY_VERB to listOf(
                "investigate",
                "uncover",
                "deduce",
                "solve",
                "suspect",
                "interrogate",
                "analyze",
                "scrutinize",
                "sleuth",
                "decode",
                "decipher",
                "examine",
                "inspect",
                "probe",
                "question",
                "query",
                "quiz",
                "grill",
                "interview",
                "cross-examine",
                "explore",
                "search",
                "hunt",
                "pursue",
                "track",
                "trail",
                "follow",
                "chase",
                "stalk",
                "shadow",
            ),
            WordType.MYSTERY_NOUN to listOf(
                "clue",
                "detective",
                "enigma",
                "riddle",
                "suspect",
                "alibi",
                "motive",
                "evidence",
                "witness",
                "conspiracy",
                "intrigue",
                "sleuth",
                "case",
                "crime",
                "puzzle",
                "mystery",
                "secret",
                "whodunit",
                "red herring",
                "suspense",
                "thriller",
                "plot",
                "solution",
                "truth",
                "lie",
                "deception",
            ),
            WordType.MYSTERY_DETECTIVE to listOf(
                "Sherlock Holmes",
                "Miss Marple",
                "Hercule Poirot",
                "Inspector Gadget",
                "Nancy Drew",
                "Sam Spade",
                "Philip Marlowe",
                "Veronica Mars",
                "Jessica Fletcher",
                "Columbo",
                "Perry Mason",
                "Adrian Monk",
            ),
            WordType.MYSTERY_CLUE to listOf(
                "fingerprint",
                "DNA evidence",
                "eyewitness testimony",
                "surveillance footage",
                "cryptic message",
                "missing item",
                "hidden message",
                "suspicious behavior",
                "unexplained phenomenon",
                "strange occurrence",
                "inexplicable event",
                "mysterious happening",
                "odd coincidence",
                "peculiar incident",
            ),
            WordType.MYSTERY_LOCATION to listOf(
                "abandoned warehouse",
                "locked room",
                "secluded mansion",
                "underground lair",
                "secret passageway",
                "hidden vault",
                "deserted alley",
                "shadowy street",
                "forgotten cellar",
                "haunted house",
                "creepy forest",
                "eerie graveyard",
                "mysterious island",
                "remote cabin",
                "isolated village",
                "enigmatic city",
                "cryptic town",
                "suspicious suburb",
                "clandestine neighborhood",
                "covert community",
                "unseen hamlet",
                "unnoticed borough",
                "unacknowledged district",
                "unrecognized quarter",
                "unmarked sector",
                "unbranded block",
                "unlabeled street",
                "unsealed avenue",
                "unopened lane",
            ),
            WordType.MYSTERY_EVIDENCE to listOf(
                "incriminating document",
                "murder weapon",
                "suspicious substance",
                "encrypted file",
                "mysterious artifact",
            ),
            WordType.MYSTERY_SUSPECT to listOf(
                "shadowy figure",
                "elusive mastermind",
                "unassuming neighbor",
                "disgruntled employee",
                "mysterious stranger"
            ),
            WordType.MYSTERY_ALIBI to listOf(
                "ironclad alibi",
                "suspicious story",
                "conflicting testimony",
                "convenient excuse",
                "verifiable whereabouts"
            ),
            WordType.MYSTERY_PHRASE to listOf(
                "the plot thickens",
                "following the trail of breadcrumbs",
                "unraveling the tangled web",
                "piecing together the puzzle"
            )
        )
        return mysteryWords[type]?.random() ?: chooseWord(type, Tense.values().random(), isPlural, null)
    }


    private fun getWesternWord(type: WordType, isPlural: Boolean = false): String {
        val westernWords = mapOf(
            WordType.ADJECTIVE to listOf(
                "rugged",
                "wild",
                "dusty",
                "frontier",
                "untamed",
                "weathered",
                "sun-baked",
                "lawless",
                "hardy",
                "gritty"
            ),
            WordType.ADVERB to listOf(
                "boldly",
                "roughly",
                "fiercely",
                "bravely",
                "sternly",
                "swiftly",
                "sharply",
                "steadily",
                "warily",
                "determinedly"
            ),
            WordType.WESTERN_VERB to listOf(
                "wrangle",
                "duel",
                "lasso",
                "rustle",
                "mosey",
                "ambush",
                "corral",
                "outdraw",
                "herd",
                "bushwhack"
            ),
            WordType.WESTERN_NOUN to listOf(
                "cowboy",
                "sheriff",
                "saloon",
                "ranch",
                "outlaw",
                "frontier",
                "cattle",
                "tumbleweed",
                "desperado",
                "homestead"
            ),
            WordType.WESTERN_CHARACTER to listOf(
                "grizzled cowhand",
                "steely-eyed gunslinger",
                "weathered prospector",
                "tough-as-nails frontier woman",
                "mysterious drifter"
            ),
            WordType.WESTERN_LOCATION to listOf(
                "dusty main street",
                "remote canyon",
                "rickety saloon",
                "sprawling cattle ranch",
                "abandoned gold mine",
                "desolate prairie"
            ),
            WordType.WESTERN_TIME to listOf("high noon", "sunset", "dawn", "dusk", "dead of night"),
            WordType.WESTERN_ITEM to listOf(
                "six-shooter",
                "lasso",
                "saddle",
                "spurs",
                "ten-gallon hat",
                "bandana",
                "wagon",
                "whiskey bottle"
            ),
            WordType.WESTERN_EXCLAMATION to listOf(
                "Yeehaw",
                "Hold your horses",
                "Well, I'll be",
                "Dadgum it",
                "Great horny toads"
            ),
            WordType.WESTERN_PHRASE to listOf(
                "faster than a rattlesnake's strike",
                "tough as old boots",
                "wild as the West Texas wind",
                "standing tall in the saddle"
            )
        )
        return westernWords[type]?.random() ?: chooseWord(type, Tense.values().random(), isPlural, null)
    }

    private fun generateTeenADHDStructure(): List<WordType> {
        val baseStructure = generateSentenceStructure().toMutableList()
        // Add interjections and repetitions
        baseStructure.add(0, WordType.INTERJECTION)
        baseStructure.add(WordType.FILLER)
        return baseStructure
    }

    private fun generatePoeticStructure(): List<WordType> {
        return listOf(
            WordType.ADJECTIVE, WordType.NOUN, WordType.VERB, WordType.ADVERB,
            WordType.PREPOSITION, WordType.ADJECTIVE, WordType.NOUN, WordType.CONJUNCTION,
            WordType.PRONOUN, WordType.VERB, WordType.ADJECTIVE, WordType.NOUN
        )
    }

    private fun generateNoirStructure(): List<WordType> {
        return listOf(
            WordType.DETERMINER, WordType.ADJECTIVE, WordType.NOUN, WordType.VERB,
            WordType.ADVERB, WordType.PREPOSITION, WordType.DETERMINER, WordType.ADJECTIVE,
            WordType.NOUN, WordType.CONJUNCTION, WordType.PRONOUN, WordType.VERB, WordType.SIMILE
        )
    }

    private fun generateFantasyStructure(): List<WordType> {
        return listOf(
            WordType.DETERMINER, WordType.ADJECTIVE, WordType.FANTASY_NOUN, WordType.FANTASY_VERB,
            WordType.ADVERB, WordType.PREPOSITION, WordType.DETERMINER, WordType.ADJECTIVE,
            WordType.FANTASY_NOUN, WordType.CONJUNCTION, WordType.PRONOUN, WordType.FANTASY_VERB
        )
    }

    private fun generateCyberpunkStructure(): List<WordType> {
        return listOf(
            WordType.DETERMINER, WordType.ADJECTIVE, WordType.CYBERPUNK_NOUN, WordType.CYBERPUNK_VERB,
            WordType.ADVERB, WordType.PREPOSITION, WordType.DETERMINER, WordType.ADJECTIVE,
            WordType.CYBERPUNK_NOUN, WordType.CONJUNCTION, WordType.PRONOUN, WordType.CYBERPUNK_VERB
        )
    }

    private fun getWordForSentenceType(type: String, wordType: WordType, isPlural: Boolean = false): String {
        return try {
            when (type) {
                "normal" -> chooseWord(wordType, Tense.values().random(), isPlural, null)
                "dramatic" -> getDramaticWord(wordType, isPlural)
                "funny" -> getFunnyWord(wordType, isPlural)
                "teenADHD" -> getTeenWord(wordType, isPlural)
                "poetic" -> getPoeticWord(wordType, isPlural)
                "scientific" -> getScientificWord(wordType, isPlural)
                "philosophical" -> getPhilosophicalWord(wordType, isPlural)
                "sarcastic" -> getSarcasticWord(wordType, isPlural)
                "noir" -> getNoirWord(wordType, isPlural)
                "fantasy" -> getFantasyWord(wordType, isPlural)
                "cyberpunk" -> getCyberpunkWord(wordType, isPlural)
                "historical" -> getHistoricalWord(wordType, isPlural)
                "romantic" -> getRomanticWord(wordType, isPlural)
                "horror" -> getHorrorWord(wordType, isPlural)
                "mystery" -> getMysteryWord(wordType, isPlural)
                "western" -> getWesternWord(wordType, isPlural)
                else -> chooseWord(wordType, Tense.values().random(), isPlural, null)
            }
        } catch (e: NoSuchElementException) {
            // Fallback to default word if the specific type doesn't have any words
            getDefaultWord(wordType)
        }
    }


    private fun getNoirWord(type: WordType, isPlural: Boolean = false): String {
        val noirWords = mapOf(
            WordType.ADJECTIVE to listOf(
                "shadowy", "mysterious", "dark", "smoky", "dangerous",
                "enigmatic", "suspicious", "grim", "cold", "haunting"
            ),

            WordType.ADVERB to listOf(
                "suspiciously", "ominously", "coldly", "grimly", "reluctantly",
                "darkly", "mysteriously", "hauntingly", "dangerously", "enigmatically"
            ),
            WordType.VERB to listOf(
                "lurk", "whisper", "vanish", "investigate", "betray",
                "conceal", "deceive", "discover", "eavesdrop", "follow"
            ),
            WordType.NOUN to listOf(
                "detective", "femme fatale", "alley", "revolver", "cigarette",
                "mystery", "crime", "shadow", "noir", "suspicion"
            ),
            WordType.SIMILE to listOf(
                "like a forgotten dream",
                "as cold as a killer's heart",
                "darker than a moonless night",
                "like a shadow in the night",
                "as mysterious as a locked room",
                "like a whisper in the wind",
                "as dangerous as a loaded gun",
                "darker than a raven's wing",
                "like a ghost in the fog",
                "as cold as a winter's chill"
            )
        )
        return noirWords[type]?.random() ?: chooseWord(type, Tense.values().random(), isPlural, null)
    }

    private fun getFantasyWord(type: WordType, isPlural: Boolean = false): String {
        val fantasyWords = mapOf(
            WordType.ADJECTIVE to listOf(
                "enchanted", "mystical", "ancient", "ethereal", "legendary",
                "mythical", "magical", "heroic", "epic", "wondrous"
            ),
            WordType.ADVERB to listOf(
                "magically", "heroically", "mysteriously", "epically", "wondrously",
                "fantastically", "legendarily"
            ),
            WordType.FANTASY_VERB to listOf(
                "conjure", "enchant", "quest", "transform", "banish",
                "summon", "vanquish", "bewitch", "enrapture", "ensnare"
            ),
            WordType.FANTASY_NOUN to listOf(
                "dragon", "wizard", "elf", "unicorn", "realm",
                "sorcerer", "sorceress", "mage", "enchantment", "spell",
                "fairy", "troll", "goblin", "dwarf", "centaur", "phoenix", "wyvern", "pegasus", "griffin"
            )
        )
        return fantasyWords[type]?.random() ?: chooseWord(type, Tense.values().random(), isPlural, null)
    }

    private fun getCyberpunkWord(type: WordType, isPlural: Boolean = false): String {
        val cyberpunkWords = mapOf(
            WordType.ADJECTIVE to listOf(
                "neon", "cybernetic", "augmented", "dystopian", "high-tech",
                "cyberpunk", "virtual", "synthetic", "artificial", "digital"
            ),
            WordType.ADVERB to listOf(
                "digitally", "virtually", "synthetically", "artificially", "electronically",
                "cybernetically", "neonly", "cyberpunkly"
            ),
            WordType.CYBERPUNK_VERB to listOf(
                "hack", "upload", "augment", "decrypt", "interface",
                "cyberjack", "neural-link", "cyberneticize", "digitize", "cyber"
            ),
            WordType.CYBERPUNK_NOUN to listOf(
                "cyborg", "netrunner", "megacorp", "AI", "neural implant",
                "cyberspace", "hacker", "cyberdeck", "cybernetics", "cybercrime", "cyberwarfare", "cyberpunk"
            )
        )
        return cyberpunkWords[type]?.random() ?: chooseWord(type, Tense.values().random(), isPlural, null)
    }


    private fun generateFunnyStructure(): List<WordType> {
        val baseStructure = generateSentenceStructure().toMutableList()
        // Add more adjectives and adverbs for comedic effect
        baseStructure.addAll(baseStructure.indices.filter { baseStructure[it] == WordType.NOUN }
            .map { WordType.ADJECTIVE })
        baseStructure.addAll(baseStructure.indices.filter { baseStructure[it] == WordType.VERB }
            .map { WordType.ADVERB })
        return baseStructure.sorted()
    }

    private val adhdPhrases = listOf(
        "wait, what was I saying?",
        "oh, speaking of which",
        "anyway, back to what I was saying",
        "random thought",
        "sorry, got distracted",
        "where was I?",
        "oh yeah, so",
        "totally forgot what I was talking about",
        "this reminds me of",
        "wait, did you see that?",
        "hold up, I just remembered",
        "okay, focus",
        "squirrel!",
        "um, what were we talking about?",
        "oh my god, I just had an idea"
    )

    private fun generatePoetricStructure(): List<WordType> {
        // Implement a poetic structure (e.g., more adjectives, metaphors)
        // This is a simplified example
        return listOf(
            WordType.ADJECTIVE, WordType.NOUN, WordType.VERB, WordType.ADVERB,
            WordType.PREPOSITION, WordType.ADJECTIVE, WordType.NOUN
        )
    }

    private fun generateScientificStructure(): List<WordType> {
        // Implement a scientific structure (e.g., more technical terms, longer sentences)
        // This is a simplified example
        return listOf(
            WordType.DETERMINER, WordType.ADJECTIVE, WordType.NOUN, WordType.VERB,
            WordType.PREPOSITION, WordType.DETERMINER, WordType.ADJECTIVE, WordType.NOUN,
            WordType.CONJUNCTION, WordType.VERB, WordType.DETERMINER, WordType.NOUN
        )
    }

    private fun generatePhilosophicalStructure(): List<WordType> {
        // Implement a philosophical structure (e.g., more abstract terms, questions)
        // This is a simplified example
        return listOf(
            WordType.DETERMINER, WordType.ADJECTIVE, WordType.NOUN, WordType.VERB,
            WordType.ADVERB, WordType.PREPOSITION, WordType.DETERMINER, WordType.NOUN,
            WordType.CONJUNCTION, WordType.PRONOUN, WordType.VERB, WordType.ADJECTIVE
        )
    }

    private fun generateSarcasticStructure(): List<WordType> {
        // Implement a sarcastic structure (e.g., exaggerated adjectives, contradictions)
        // This is a simplified example
        return listOf(
            WordType.ADVERB, WordType.ADJECTIVE, WordType.NOUN, WordType.VERB,
            WordType.DETERMINER, WordType.ADJECTIVE, WordType.NOUN, WordType.CONJUNCTION,
            WordType.PRONOUN, WordType.ADVERB, WordType.VERB
        )
    }


    private fun getTeenWord(type: WordType, isPlural: Boolean = false): String {
        return when (type) {
            WordType.ADJECTIVE -> teenSlang.random()
            WordType.ADVERB -> "like, ${funnyAdverbs.random()}"
            WordType.VERB -> funnyVerbs.random()
            WordType.NOUN -> {
                val noun = funnyNouns.random()
                if (isPlural && !pluralNouns.contains(noun)) getPlural(noun) else noun
            }

            else -> wordList.filter { it.type == type }.random().value
        }
    }

    private fun addTeenADHDElement(): String {
        return when (Random.nextInt(3)) {
            0 -> teenSlang.random()
            1 -> adhdPhrases.random()
            else -> addFunnyElement()
        }
    }

    fun randomTeenADHDSentence(): String {
        val structure = generateSentenceStructure()
        val words = mutableListOf<String>()
        var isPlural = false
        var subject: String? = null
        val tense = Tense.values().random()

        structure.forEach { wordType ->
            val word = when (wordType) {
                WordType.DETERMINER -> {
                    val determiner = getTeenWord(wordType)
                    isPlural = determiner in listOf("these", "those", "some", "many")
                    determiner
                }

                WordType.PRONOUN -> {
                    val pronoun = getTeenWord(wordType)
                    isPlural = pronoun in listOf("we", "they", "you")
                    subject = pronoun
                    pronoun
                }

                WordType.VERB -> conjugateVerb(getTeenWord(wordType), tense, isPlural, subject)
                WordType.NOUN -> if (isPlural) getPlural(getTeenWord(wordType)) else getTeenWord(wordType)
                else -> getTeenWord(wordType)
            }
            words.add(word)
        }

        var sentence = words.joinToString(" ")
        sentence = sentence.replaceFirstChar { it.uppercase() }

        // Add teen ADHD elements
        if (Random.nextFloat() < 0.5) {
            sentence += ", ${addTeenADHDElement()}"
        }

        // Add filler words and repetitions
        val fillerWords = listOf("like", "um", "uh", "you know", "literally", "basically", "I mean")
        sentence = sentence.split(" ").flatMap {
            if (Random.nextFloat() < 0.2) listOf(fillerWords.random(), it)
            else if (Random.nextFloat() < 0.1) listOf(it, it) // Repetition
            else listOf(it)
        }.joinToString(" ")

        // Sometimes start with "Oh my god" or "Dude"
        if (Random.nextFloat() < 0.3) {
            sentence = "${listOf("Oh my god", "Dude", "Yo").random()}, $sentence"
        }

        return postProcessSentence(sentence)
    }

    fun randomTeenADHDParagraph(sentenceCount: Int = 3): String {
        return (1..sentenceCount).map { randomTeenADHDSentence() }.joinToString(" ")
    }

    private fun preProcessSentence(words: List<String>): List<String> {
        return words.mapIndexed { index, word ->
            when {
                word == "a" && index < words.size - 1 && words[index + 1].first().toLowerCase() in "aeiou" -> "an"
                word == "an" && index < words.size - 1 && words[index + 1].first().toLowerCase() !in "aeiou" -> "a"
                else -> word
            }
        }
    }


    private fun postProcessSentence(sentence: String): String {
        var processedSentence = sentence

        // Handle contractions
        val contractions = mapOf(
            "I am" to "I'm",
            "you are" to "you're",
            "he is" to "he's",
            "she is" to "she's",
            "it is" to "it's",
            "we are" to "we're",
            "they are" to "they're",
            "is not" to "isn't",
            "are not" to "aren't",
            "was not" to "wasn't",
            "were not" to "weren't",
            "have not" to "haven't",
            "has not" to "hasn't",
            "had not" to "hadn't",
            "will not" to "won't",
            "would not" to "wouldn't",
            "do not" to "don't",
            "does not" to "doesn't",
            "did not" to "didn't",
            "cannot" to "can't"
        )

        for ((full, contraction) in contractions) {
            processedSentence = processedSentence.replace(full, contraction)
        }

        // Add commas for compound sentences
        val conjunctions = listOf("and", "but", "or", "yet", "for", "nor", "so")
        conjunctions.forEach { conjunction ->
            val regex = "\\b$conjunction\\b".toRegex()
            if (regex.containsMatchIn(processedSentence)) {
                processedSentence = regex.replace(processedSentence, ", $conjunction")
                //removes the space before the comma
                processedSentence = processedSentence.replace(" , ", ", ")
                processedSentence = processedSentence.replace(",,", ",")

            }
        }

        // Add commas after introductory phrases
        val introductoryPhrases = listOf(
            "However",
            "Moreover",
            "Furthermore",
            "In addition",
            "On the other hand",
            "Therefore",
            "Nonetheless"
        )
        introductoryPhrases.forEach { phrase ->
            if (processedSentence.startsWith(phrase)) {
                processedSentence = processedSentence.replaceFirst(phrase, "$phrase,")
            }
        }

        // Ensure proper capitalization and punctuation
        processedSentence = processedSentence.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        if (!processedSentence.endsWith(".") && !processedSentence.endsWith("!") && !processedSentence.endsWith("?")) {
            processedSentence += if (Random.nextFloat() < 0.3) "!" else "."
        }

        return processedSentence
    }

    private fun conjugateVerb(verb: String, tense: Tense, isPlural: Boolean, subject: String?): String {
        val irregularForms = irregularVerbs[verb]
        return when (tense) {
            Tense.PRESENT -> when {
                irregularForms != null -> irregularForms["present"] ?: verb
                isPlural || subject in listOf("I", "you", "we", "they") -> verb
                else -> "${verb}s"
            }

            Tense.PAST -> irregularForms?.get("past") ?: "${verb}ed"
            Tense.FUTURE -> "will $verb"
        }
    }

    private fun getPlural(noun: String): String {
        return when {
            pluralNouns.contains(noun) -> noun
            noun.endsWith("s") || noun.endsWith("x") || noun.endsWith("z") || noun.endsWith("ch") || noun.endsWith("sh") -> "${noun}es"
            noun.endsWith("y") && noun.length > 1 && noun[noun.length - 2] !in "aeiou" -> "${noun.dropLast(1)}ies"
            else -> "${noun}s"
        }
    }

    private fun chooseWord(type: WordType, tense: Tense, isPlural: Boolean, subject: String?): String {
        val words = wordList.filter { it.type == type }
        return if (words.isNotEmpty()) {
            val word = words.random().value
            when (type) {
                WordType.VERB -> conjugateVerb(word, tense, isPlural, subject)
                WordType.NOUN -> if (isPlural) getPlural(word) else word
                else -> word
            }
        } else {
            // Fallback: return a default word for the type
            getDefaultWord(type)
        }
    }

    private fun getDefaultWord(type: WordType): String {
        return when (type) {
            WordType.NOUN -> "thing"
            WordType.VERB -> "do"
            WordType.ADJECTIVE -> "random"
            WordType.ADVERB -> "randomly"
            WordType.PREPOSITION -> "with"
            WordType.CONJUNCTION -> "and"
            WordType.PRONOUN -> "it"
            WordType.DETERMINER -> "the"
            WordType.INTERJECTION -> "oh"
            WordType.FILLER -> "um"
            else -> "word"
        }
    }

    private fun generateSentenceStructure(): List<WordType> {
        return sentenceStructures.random()
    }

    private fun generateDramaticStructure(): List<WordType> {
        val baseStructure: MutableList<WordType> = sentenceStructures.random().toMutableList()

        // Add more adjectives and adverbs for dramatic effect
        baseStructure.addAll(baseStructure.indices.filter { baseStructure[it] == WordType.NOUN }
            .map { WordType.ADJECTIVE } + baseStructure.indices.filter { baseStructure[it] == WordType.VERB }
            .map { WordType.ADVERB })

        return baseStructure.sorted()
    }


    private fun getDramaticWord(type: WordType, isPlural: Boolean = false): String {
        val dramaticWords = mapOf(
            WordType.ADJECTIVE to dramaticAdjectives,
            WordType.ADVERB to dramaticAdverbs,
            WordType.VERB to dramaticVerbs,
            WordType.NOUN to wordList.filter { it.type == WordType.NOUN }.map { it.value }
        )

        return dramaticWords[type]?.randomOrNull()
            ?: chooseWord(type, Tense.values().random(), isPlural, null)
    }


    private fun addDramaticElement(): String {
        val elements = listOf(
            "leaving a profound impact",
            "creating an unforgettable impression",
            "echoing through time",
            "defying all expectations",
            "changing everything forever",
            "revealing hidden truths",
            "igniting a spark of hope",
            "shattering the silence",
            "unveiling a new reality"
        )
        return elements.random()
    }

    fun randomDramaticSentence(): String {
        val structure = generateDramaticStructure()
        val words = mutableListOf<String>()
        var isPlural = false
        var subject: String? = null
        val tense = Tense.values().random()

        structure.forEach { wordType ->
            val word = when (wordType) {
                WordType.DETERMINER -> {
                    val determiner = getDramaticWord(wordType)
                    isPlural = determiner in listOf("these", "those", "some", "many")
                    determiner
                }

                WordType.PRONOUN -> {
                    val pronoun = getDramaticWord(wordType)
                    isPlural = pronoun in listOf("we", "they", "you")
                    subject = pronoun
                    pronoun
                }

                WordType.VERB -> conjugateVerb(getDramaticWord(wordType), tense, isPlural, subject)
                WordType.NOUN -> if (isPlural) getPlural(getDramaticWord(wordType)) else getDramaticWord(wordType)
                else -> getDramaticWord(wordType)
            }
            words.add(word)
        }

        var sentence = words.joinToString(" ")
        sentence = sentence.replaceFirstChar { it.uppercase() }

        // Add dramatic elements with better flow
        if (Random.nextFloat() < 0.3) {
            sentence += ", ${addDramaticElement()}"
        }

        // Add dramatic interjections
        val dramaticInterjections = listOf(
            "In a twist of fate,",
            "Against all odds,",
            "In a moment of truth,",
            "With bated breath,",
            "In the blink of an eye,",
            "Amidst the chaos,",
            "As destiny unfolded,",
            "In a stroke of genius,",
            "With unwavering resolve,"
        )
        if (Random.nextFloat() < 0.2) {
            sentence = "${dramaticInterjections.random()} $sentence"
        }

        return postProcessSentence(sentence)
    }

    fun randomDramaticParagraph(sentenceCount: Int = 3): String {
        return (1..sentenceCount).map { randomDramaticSentence() }.joinToString(" ")
    }

    private val funnyAdjectives = listOf(
        "hilarious",
        "wacky",
        "zany",
        "goofy",
        "goodness gracious",
        "absurd",
        "ridiculous",
        "blasphemous",
        "outrageous",
        "goofy ahh",
        "silly",
        "silly",
        "absurd",
        "ridiculous",
        "preposterous",
        "ludicrous",
        "comical",
        "whimsical",
        "quirky",
        "bizarre",
        "outlandish",
        "peculiar",
        "bonkers",
        "kooky",
        "nutty",
        "offbeat",
        "wacko",
        "kooky",
        "loony",
    )

    private val funnyNouns = listOf(
        "clown",
        "banana",
        "unicorn",
        "pickle",
        "flamingo",
        "ninja",
        "spaghetti",
        "llama",
        "toaster",
        "octopus",
        "pirate",
        "sasquatch",
        "wizard",
        "moustache",
        "chicken",
        "penguin",
        "koala",
        "gyatt",
        "platypus",
        "narwhal",

        )

    private val funnyVerbs = listOf(
        "juggle",
        "tickle",
        "wobble",
        "giggle",
        "waddle",
        "squawk",
        "burp",
        "hiccup",
        "tumble",
        "sneeze",
        "wiggle",
        "snort",
        "bumble",
        "dance",
        "prance",
        "sizzle",
        "fizzle",
        "whirl",
        "twirl",
        "scoot",
        "splat",
        "splatter",
    )

    private val funnyAdverbs = listOf(
        "hilariously",
        "goofily",
        "awkwardly",
        "clumsily",
        "ridiculously",
        "comically",
        "absurdly",
        "sillily",
        "bizarrely",
        "whimsically",
        "peculiarly",
        "zanily",
        "preposterously",
        "ludicrously",
        "outrageously",
        "ridiculously"
    )

    private fun getFunnyWord(type: WordType, isPlural: Boolean = false): String {
        return when (type) {
            WordType.ADJECTIVE -> funnyAdjectives.random()
            WordType.ADVERB -> funnyAdverbs.random()
            WordType.VERB -> funnyVerbs.random()
            WordType.NOUN -> {
                val noun = funnyNouns.random()
                if (isPlural && !pluralNouns.contains(noun)) getPlural(noun) else noun
            }

            else -> wordList.filter { it.type == type }.random().value
        }
    }

    private fun addFunnyElement(): String {
        val elements = listOf(
            "while wearing a tutu",
            "with a banana peel on their head",
            "in slow motion",
            "like a chicken doing the Macarena",
            "as if their pants were on fire",
            "while riding a unicycle",
            "with a pie in their face",
            "like a cat chasing a laser pointer",
            "as if they just saw a ghost wearing sunglasses"
        )
        return elements.random()
    }

    fun randomFunnySentence(): String {
        val structure = generateSentenceStructure()
        val words = mutableListOf<String>()
        var isPlural = false
        var subject: String? = null
        val tense = Tense.values().random()

        structure.forEach { wordType ->
            val word = when (wordType) {
                WordType.DETERMINER -> {
                    val determiner = getFunnyWord(wordType)
                    isPlural = determiner in listOf("these", "those", "some", "many")
                    determiner
                }

                WordType.PRONOUN -> {
                    val pronoun = getFunnyWord(wordType)
                    isPlural = pronoun in listOf("we", "they", "you")
                    subject = pronoun
                    pronoun
                }

                WordType.VERB -> conjugateVerb(getFunnyWord(wordType), tense, isPlural, subject)
                WordType.NOUN -> if (isPlural) getPlural(getFunnyWord(wordType)) else getFunnyWord(wordType)
                else -> getFunnyWord(wordType)
            }
            words.add(word)
        }

        var sentence = words.joinToString(" ")
        sentence = sentence.replaceFirstChar { it.uppercase() }

        // Add funny elements
        if (Random.nextFloat() < 0.3) {
            sentence += " ${addFunnyElement()}"
        }

        // Add funny interjections
        val funnyInterjections = listOf(
            "Holy guacamole!",
            "Great Scott!",
            "Zoinks!",
            "Well, butter my biscuit!",
            "Holy moly!",
            "Jumpin' Jehoshaphat!",
            "Leapin' lizards!",
            "Golly gee whiz!",
            "Sweet mother of monkey milk!"
        )
        if (Random.nextFloat() < 0.2) {
            sentence = "${funnyInterjections.random()} $sentence"
        }

        return postProcessSentence(sentence)
    }

    fun randomFunnyParagraph(sentenceCount: Int = 3): String {
        return (1..sentenceCount).map { randomFunnySentence() }.joinToString(" ")
    }


    private fun getPoeticWord(type: WordType, isPlural: Boolean = false): String {
        // Implement poetic word selection
        // This is a simplified example
        val poeticWords = mapOf(
            WordType.ADJECTIVE to listOf("ethereal", "melancholic", "whimsical", "ephemeral"),
            WordType.ADVERB to listOf("wistfully", "serenely", "hauntingly", "tenderly"),
            WordType.VERB to listOf("whisper", "bloom", "fade", "dance"),
            WordType.NOUN to listOf("moonlight", "shadow", "dream", "whisper")
        )
        return poeticWords[type]?.random() ?: chooseWord(type, Tense.values().random(), isPlural, null)
    }

    private fun getScientificWord(type: WordType, isPlural: Boolean = false): String {
        // Implement scientific word selection
        // This is a simplified example
        val scientificWords = mapOf(
            WordType.ADJECTIVE to listOf("quantum", "molecular", "empirical", "theoretical"),
            WordType.ADVERB to listOf("systematically", "empirically", "theoretically", "quantitatively"),
            WordType.VERB to listOf("hypothesize", "analyze", "synthesize", "observe"),
            WordType.NOUN to listOf("paradigm", "theory", "hypothesis", "data")
        )
        return scientificWords[type]?.random() ?: chooseWord(type, Tense.values().random(), isPlural, null)
    }

    private fun getPhilosophicalWord(type: WordType, isPlural: Boolean = false): String {
        // Implement philosophical word selection
        // This is a simplified example
        val philosophicalWords = mapOf(
            WordType.ADJECTIVE to listOf("existential", "metaphysical", "ontological", "epistemological"),
            WordType.ADVERB to listOf("profoundly", "inherently", "fundamentally", "essentially"),
            WordType.VERB to listOf("contemplate", "ponder", "postulate", "deduce"),
            WordType.NOUN to listOf("existence", "essence", "truth", "reality")
        )
        return philosophicalWords[type]?.random() ?: chooseWord(type, Tense.values().random(), isPlural, null)
    }

    private val weightedSentenceTypes: List<Pair<String, Double>> = calculateWeights()

    private fun calculateWeights(): List<Pair<String, Double>> {
        val totalWeight = sentenceTypes.indices.sumOf { 1.0 / ln(it + Math.E) }
        var cumulativeWeight = 0.0
        return sentenceTypes.mapIndexed { index, type ->
            val weight = 1.0 / ln(index + Math.E) / totalWeight
            cumulativeWeight += weight
            type to cumulativeWeight
        }
    }

    fun getRandomSentenceType(): String {
        val randomValue = Random.nextDouble()
        return weightedSentenceTypes.first { (_, cumulativeWeight) ->
            randomValue <= cumulativeWeight
        }.first
    }

    private fun getSarcasticWord(type: WordType, isPlural: Boolean = false): String {
        // Implement sarcastic word selection
        // This is a simplified example
        val sarcasticWords = mapOf(
            WordType.ADJECTIVE to listOf("brilliant", "fantastic", "genius", "flawless"),
            WordType.ADVERB to listOf("obviously", "clearly", "undoubtedly", "absolutely"),
            WordType.VERB to listOf("amaze", "impress", "dazzle", "stun"),
            WordType.NOUN to listOf("masterpiece", "genius", "marvel", "wonder")
        )
        return sarcasticWords[type]?.random() ?: chooseWord(type, Tense.values().random(), isPlural, null)
    }

    //    fun randomSentence(): String {
//        val sentenceType = getRandomSentenceType()
//        val structure = when (sentenceType) {
//            "normal" -> generateSentenceStructure()
//            "dramatic" -> generateDramaticStructure()
//            "funny" -> generateFunnyStructure()
//            "teenADHD" -> generateTeenADHDStructure()
//            "poetic" -> generatePoeticStructure()
//            "scientific" -> generateScientificStructure()
//            "philosophical" -> generatePhilosophicalStructure()
//            "sarcastic" -> generateSarcasticStructure()
//            "noir" -> generateNoirStructure()
//            "fantasy" -> generateFantasyStructure()
//            "cyberpunk" -> generateCyberpunkStructure()
//            "historical" -> generateHistoricalStructure()
//            "romantic" -> generateRomanticStructure()
//            "horror" -> generateHorrorStructure()
//            "mystery" -> generateMysteryStructure()
//            "western" -> generateWesternStructure()
//            else -> generateSentenceStructure()
//        }
//
//        val words = mutableListOf<String>()
//        var isPlural = false
//        var subject: String? = null
//        val tense = Tense.values().random()
//
//        structure.forEach { wordType ->
//            val word = when (wordType) {
//                WordType.DETERMINER -> {
//                    val determiner = getWordForSentenceType(sentenceType, WordType.DETERMINER)
//                    isPlural = determiner in listOf("these", "those", "some", "many")
//                    determiner
//                }
//
//                WordType.PRONOUN -> {
//                    subject = getWordForSentenceType(sentenceType, WordType.PRONOUN)
//                    isPlural = subject in listOf("we", "they", "you")
//                    subject
//                }
//
//                WordType.VERB, WordType.FANTASY_VERB, WordType.CYBERPUNK_VERB,
//                WordType.HISTORICAL_VERB, WordType.ROMANTIC_VERB, WordType.HORROR_VERB,
//                WordType.MYSTERY_VERB, WordType.WESTERN_VERB ->
//                    conjugateVerb(getWordForSentenceType(sentenceType, wordType), tense, isPlural, subject)
//
//                WordType.NOUN, WordType.FANTASY_NOUN, WordType.CYBERPUNK_NOUN,
//                WordType.HISTORICAL_NOUN, WordType.ROMANTIC_NOUN, WordType.HORROR_NOUN,
//                WordType.MYSTERY_NOUN, WordType.WESTERN_NOUN ->
//                    if (isPlural) getPlural(getWordForSentenceType(sentenceType, wordType))
//                    else getWordForSentenceType(sentenceType, wordType)
//
//                else -> getWordForSentenceType(sentenceType, wordType)
//            }
//            word?.let { words.add(it) }
//        }
//
//        val preprocessedWords = preProcessSentence(words)
//        var rawSentence = preprocessedWords.joinToString(" ")
//
//        // Add a chance for run-on sentences
//        if (Random.nextFloat() < 0.2) {
//            val connectingWords = listOf("and", "but", "so", "yet", "for", "nor", "or")
//            val secondSentence = randomSentence()
//            rawSentence += " ${connectingWords.random()} $secondSentence"
//        }
//
//        return postProcessSentence(rawSentence)
//    }
    @JvmStatic
    fun randomSentence(): String {
        val sentenceType = getRandomSentenceType()
        val structure = generateStructureForType(sentenceType)

        val words = mutableListOf<String>()
        var isPlural = false
        var subject: String? = null
        val tense = Tense.values().random()

        structure.forEach { wordType ->
            val word = when (wordType) {
                WordType.DETERMINER -> {
                    val determiner = getWordForSentenceType(sentenceType, WordType.DETERMINER)
                    isPlural = determiner in listOf("these", "those", "some", "many")
                    determiner
                }

                WordType.PRONOUN -> {
                    subject = getWordForSentenceType(sentenceType, WordType.PRONOUN)
                    isPlural = subject in listOf("we", "they", "you")
                    subject
                }

                WordType.VERB, WordType.FANTASY_VERB, WordType.CYBERPUNK_VERB,
                WordType.HISTORICAL_VERB, WordType.ROMANTIC_VERB, WordType.HORROR_VERB,
                WordType.MYSTERY_VERB, WordType.WESTERN_VERB ->
                    conjugateVerb(getWordForSentenceType(sentenceType, wordType), tense, isPlural, subject)

                WordType.NOUN, WordType.FANTASY_NOUN, WordType.CYBERPUNK_NOUN,
                WordType.HISTORICAL_NOUN, WordType.ROMANTIC_NOUN, WordType.HORROR_NOUN,
                WordType.MYSTERY_NOUN, WordType.WESTERN_NOUN ->
                    if (isPlural) getPlural(getWordForSentenceType(sentenceType, wordType))
                    else getWordForSentenceType(sentenceType, wordType)

                else -> getWordForSentenceType(sentenceType, wordType)
            }
            word?.let { words.add(it) }
        }

        val preprocessedWords = preProcessSentence(words)
        var rawSentence = preprocessedWords.joinToString(" ")

        // Add a chance for run-on sentences
        if (Random.nextFloat() < 0.2) {
            val connectingWords = listOf("and", "but", "so", "yet", "for", "nor", "or")
            val secondSentence = randomSentence()
            rawSentence += " ${connectingWords.random()} $secondSentence"
        }

        return postProcessSentence(rawSentence)
    }


}
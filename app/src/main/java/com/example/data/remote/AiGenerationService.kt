package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ProjectEntity
import com.example.data.model.SceneEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AiGenerationService {

    private val geminiService = GeminiClient.apiService
    private val apiKey = try {
        BuildConfig.GEMINI_API_KEY
    } catch (e: Throwable) {
        ""
    }

    private suspend fun callGemini(prompt: String): String? {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return null
        }
        return try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.75f,
                    maxOutputTokens = 3500
                )
            )
            val response = geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        } catch (e: Exception) {
            Log.e("AiGenerationService", "Gemini API error: ${e.message}")
            null
        }
    }

    suspend fun generateStory(
        idea: String,
        characters: String,
        location: String,
        mood: String,
        ending: String,
        specialInstructions: String,
        language: String,
        style: String,
        videoType: String,
        targetDuration: String,
        actionType: String = "GENERATE", // GENERATE, IMPROVE, SHORTEN, EXPAND, REGENERATE
        currentStory: String = ""
    ): String = withContext(Dispatchers.Default) {
        val langInstruction = when (language) {
            "Hindi" -> "Write in natural, engaging conversational Hindi (Devanagari script)."
            "Urdu" -> "Write in poetic, emotional, natural Urdu (Arabic script or Nastaliq)."
            else -> "Write in crisp, cinematic, engaging English."
        }

        val prompt = when (actionType) {
            "IMPROVE" -> """
                You are an expert AI Screenwriter and Storyteller.
                Improve and enhance the following story. Boost narrative tension, sensory descriptions, dialogue punch, and visual dynamism suitable for a $videoType ($targetDuration).
                Visual Style: $style
                Mood: $mood
                $langInstruction
                Original Story:
                $currentStory
                Provide only the improved story text directly.
            """.trimIndent()

            "SHORTEN" -> """
                You are an expert AI Screenwriter. Shorten the following story so it fits perfectly in a fast-paced $videoType ($targetDuration).
                Keep the core emotional climax, key characters, and snappy rhythm.
                $langInstruction
                Original Story:
                $currentStory
                Provide only the condensed story text directly.
            """.trimIndent()

            "EXPAND" -> """
                You are an expert AI Screenwriter. Expand the following story with richer sensory details, character motivations, atmosphere, and cinematic beats for $videoType.
                Visual Style: $style
                Mood: $mood
                $langInstruction
                Original Story:
                $currentStory
                Provide only the expanded story text directly.
            """.trimIndent()

            else -> """
                You are an expert AI Screenwriter and Storyteller creating a script for a $videoType with target duration $targetDuration.
                Story Idea: $idea
                Characters: $characters
                Location / Setting: $location
                Mood / Tone: $mood
                Ending Style: $ending
                Special Instructions: $specialInstructions
                Visual Style: $style
                Language Requirement: $langInstruction

                Create a gripping, visually descriptive story structured with a strong hook, rising action, intense climax, and satisfying ending.
                Keep it perfectly tailored for video creation. Output ONLY the story text.
            """.trimIndent()
        }

        val geminiResult = callGemini(prompt)
        if (!geminiResult.isNullOrBlank()) {
            return@withContext geminiResult.trim()
        }

        // Context-aware smart fallback generator
        generateFallbackStory(
            idea = if (idea.isNotBlank()) idea else "एक अनसुनी रहस्यमयी दास्तान",
            characters = characters,
            location = location,
            mood = mood,
            ending = ending,
            language = language,
            style = style,
            actionType = actionType,
            currentStory = currentStory
        )
    }

    suspend fun generateScenes(
        project: ProjectEntity,
        storyText: String,
        numScenes: Int
    ): List<SceneEntity> = withContext(Dispatchers.Default) {
        val effectiveNumScenes = if (numScenes in 1..20) numScenes else project.numScenes
        val langInstruction = when (project.language) {
            "Hindi" -> "Hindi (Devanagari)"
            "Urdu" -> "Urdu"
            else -> "English"
        }

        val prompt = """
            You are a master film director and AI video prompt architect.
            Divide the following story into exactly $effectiveNumScenes structured scenes for a ${project.videoType} (${project.targetDuration}) in ${project.style} style.
            Language for dialogue and voice-over: $langInstruction.
            Story:
            $storyText

            Return a valid JSON array of $effectiveNumScenes objects with EXACTLY these string keys:
            [
              {
                "sceneNumber": 1,
                "sceneTitle": "...",
                "sceneDescription": "...",
                "characters": "...",
                "location": "...",
                "characterActions": "...",
                "emotion": "...",
                "cameraShot": "...",
                "lighting": "...",
                "background": "...",
                "dialogue": "...",
                "voiceOver": "...",
                "duration": "10s"
              }
            ]
            Output ONLY the valid JSON array without markdown formatting.
        """.trimIndent()

        val geminiResult = callGemini(prompt)
        if (!geminiResult.isNullOrBlank()) {
            try {
                val cleanedJson = geminiResult.replace("```json", "").replace("```", "").trim()
                val jsonArray = JSONArray(cleanedJson)
                val resultList = mutableListOf<SceneEntity>()
                val perSceneSec = maxOf(5, (parseDurationSeconds(project.targetDuration) / effectiveNumScenes))
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    resultList.add(
                        SceneEntity(
                            projectId = project.id,
                            sceneNumber = obj.optInt("sceneNumber", i + 1),
                            sceneTitle = obj.optString("sceneTitle", "Scene ${i + 1}"),
                            sceneDescription = obj.optString("sceneDescription", ""),
                            characters = obj.optString("characters", project.characters.ifBlank { "Main Protagonist" }),
                            location = obj.optString("location", project.location.ifBlank { "Cinematic Environment" }),
                            characterActions = obj.optString("characterActions", ""),
                            emotion = obj.optString("emotion", project.mood),
                            cameraShot = obj.optString("cameraShot", "Medium cinematic shot"),
                            lighting = obj.optString("lighting", "Golden hour cinematic glow"),
                            background = obj.optString("background", "Detailed atmospheric backdrop"),
                            dialogue = obj.optString("dialogue", ""),
                            voiceOver = obj.optString("voiceOver", ""),
                            duration = "${perSceneSec}s"
                        )
                    )
                }
                if (resultList.isNotEmpty()) {
                    return@withContext resultList
                }
            } catch (e: Exception) {
                Log.e("AiGenerationService", "Failed to parse scenes JSON: ${e.message}")
            }
        }

        // Smart Fallback Scene Generator
        generateFallbackScenes(project, storyText, effectiveNumScenes)
    }

    suspend fun generateImagePrompt(
        project: ProjectEntity,
        scene: SceneEntity,
        characterReference: String = ""
    ): String = withContext(Dispatchers.Default) {
        val charRef = if (characterReference.isNotBlank()) characterReference else project.characterRef

        val prompt = """
            You are a world-class AI Image Prompt Engineer (Midjourney v6, FLUX.1, Imagen 3).
            Create an ultra-detailed, photorealistic text-to-image prompt for:
            Scene Number: ${scene.sceneNumber} (${scene.sceneTitle})
            Visual Style: ${project.style}
            Character Consistency Reference: $charRef
            Characters: ${scene.characters}
            Location & Environment: ${scene.location}
            Character Action & Pose: ${scene.characterActions}
            Emotion & Expression: ${scene.emotion}
            Camera Shot & Lens: ${scene.cameraShot}
            Lighting & Atmosphere: ${scene.lighting}
            Background Details: ${scene.background}

            Format the prompt into a comprehensive, highly descriptive continuous prompt covering:
            - Exact character appearance (face, hair, eyes, skin texture, age, clothes, body position)
            - Precise emotion and dynamic action
            - Atmospheric environment, location architecture, time of day, weather, volumetric lighting
            - Camera equipment (e.g. Arri Alexa Mini, 35mm anamorphic lens, shallow depth of field, f/1.8, ISO 100)
            - Cinematic composition, color grading, photorealistic 8K render, ray tracing details.
            Ensure character appearance remains strictly consistent with the reference.
            Output ONLY the ready-to-use image prompt.
        """.trimIndent()

        val geminiResult = callGemini(prompt)
        if (!geminiResult.isNullOrBlank()) {
            return@withContext geminiResult.trim()
        }

        // Smart Fallback
        buildFallbackImagePrompt(project, scene, charRef)
    }

    suspend fun generateVideoPrompt(
        project: ProjectEntity,
        scene: SceneEntity,
        characterReference: String = ""
    ): Pair<String, String> = withContext(Dispatchers.Default) {
        val charRef = if (characterReference.isNotBlank()) characterReference else project.characterRef

        val prompt = """
            You are an expert AI Video Prompt Director for video generative models like Google Veo, Runway Gen-3, Kling, and Luma Dream Machine.
            Generate:
            1. A comprehensive natural language cinematic video prompt detailing:
               - Scene description and character action
               - Fluid body movement and micro-expressions
               - Camera movement (e.g. slow pan left, steadycam push-in, dynamic crane tilt)
               - Lighting shifts and environmental physics (wind, dust, water, fabric movement)
               - Dialogue and lip-sync instruction
               - Voice emotion, sound effects (SFX), and background audio ambience
               - Continuity instructions matching previous scenes
            2. A strictly formatted JSON representation with fields:
               {
                 "scene": ${scene.sceneNumber},
                 "duration": "${scene.duration}",
                 "characters": "...",
                 "action": "...",
                 "dialogue": "...",
                 "emotion": "...",
                 "camera": "...",
                 "lighting": "...",
                 "environment": "...",
                 "audio": "...",
                 "lip_sync": "...",
                 "visual_style": "...",
                 "continuity": "..."
               }

            Return your response in this exact format:
            ===TEXT_PROMPT===
            <detailed text video prompt here>
            ===JSON_PROMPT===
            <valid json object here>
        """.trimIndent()

        val geminiResult = callGemini(prompt)
        if (!geminiResult.isNullOrBlank() && geminiResult.contains("===TEXT_PROMPT===")) {
            try {
                val parts = geminiResult.split("===JSON_PROMPT===")
                val textPrompt = parts[0].replace("===TEXT_PROMPT===", "").trim()
                val rawJson = parts.getOrNull(1)?.replace("```json", "")?.replace("```", "")?.trim() ?: ""
                val cleanJson = if (rawJson.startsWith("{")) rawJson else buildFallbackVideoJson(project, scene, charRef)
                return@withContext Pair(textPrompt, cleanJson)
            } catch (e: Exception) {
                Log.e("AiGenerationService", "Error parsing video prompt: ${e.message}")
            }
        }

        // Fallback
        val textFallback = buildFallbackVideoTextPrompt(project, scene, charRef)
        val jsonFallback = buildFallbackVideoJson(project, scene, charRef)
        Pair(textFallback, jsonFallback)
    }

    suspend fun generateVoiceScript(
        project: ProjectEntity,
        scenes: List<SceneEntity>,
        voiceStyle: String,
        language: String
    ): String = withContext(Dispatchers.Default) {
        val scenesSummary = scenes.joinToString("\n\n") {
            "Scene ${it.sceneNumber} (${it.sceneTitle}): Action: ${it.characterActions}. Dialogue: ${it.dialogue}. VoiceOver: ${it.voiceOver}"
        }

        val prompt = """
            You are a professional Voice-Over Director and Scriptwriter.
            Convert the following scenes into a complete, synchronized voice-over script.
            Target Language: $language
            Voice Style: $voiceStyle (e.g., Emotional, Storytelling, Energetic, Calm, Cinematic)

            Scenes:
            $scenesSummary

            For each scene provide:
            - Scene Number
            - Emotion / Tone tag: [Emotion: ...]
            - Pacing / Pause instructions: [Pause 1.5s], [Whisper], [Deep breath]
            - Natural Voice-Over Text written in $language with high engagement and emotional resonance.

            Output ONLY the formatted voice-over script.
        """.trimIndent()

        val geminiResult = callGemini(prompt)
        if (!geminiResult.isNullOrBlank()) {
            return@withContext geminiResult.trim()
        }

        // Fallback
        buildFallbackVoiceScript(scenes, voiceStyle, language)
    }

    suspend fun generateYouTubePackage(
        project: ProjectEntity,
        scenes: List<SceneEntity>,
        storyText: String
    ): Map<String, String> = withContext(Dispatchers.Default) {
        val prompt = """
            You are an expert YouTube Shorts and Video SEO Strategist.
            Create an upload-ready metadata package for this video:
            Title Idea / Topic: ${project.name}
            Style: ${project.style}
            Target Duration: ${project.targetDuration}
            Story / Content:
            $storyText

            Generate the following sections with high CTR and engaging hooks without misleading clickbait:
            1. Main YouTube Title (Catchy, punchy, includes emoji)
            2. 5 Alternative Titles (for A/B testing)
            3. Engaging Description (Hook, summary, timestamps/chapters, disclaimer)
            4. Hashtags (Top 10 trending hashtags e.g. #Shorts #AIVideo ...)
            5. Keywords / Tags (Comma separated list of 25 search tags)
            6. Short Caption (Ideal for Instagram Reels / TikTok / Shorts feed)
            7. Thumbnail Text (Bold, high-curiosity 3-4 word phrase)
            8. Pinned Comment (High interaction conversation starter)

            Return as valid JSON with keys:
            {
              "title": "...",
              "altTitles": "...",
              "description": "...",
              "hashtags": "...",
              "tags": "...",
              "shortCaption": "...",
              "thumbnailText": "...",
              "pinnedComment": "..."
            }
        """.trimIndent()

        val geminiResult = callGemini(prompt)
        if (!geminiResult.isNullOrBlank()) {
            try {
                val cleanedJson = geminiResult.replace("```json", "").replace("```", "").trim()
                val json = JSONObject(cleanedJson)
                return@withContext mapOf(
                    "title" to json.optString("title"),
                    "altTitles" to json.optString("altTitles"),
                    "description" to json.optString("description"),
                    "hashtags" to json.optString("hashtags"),
                    "tags" to json.optString("tags"),
                    "shortCaption" to json.optString("shortCaption"),
                    "thumbnailText" to json.optString("thumbnailText"),
                    "pinnedComment" to json.optString("pinnedComment")
                )
            } catch (e: Exception) {
                Log.e("AiGenerationService", "Error parsing YouTube package JSON: ${e.message}")
            }
        }

        // Fallback
        buildFallbackYouTubePackage(project, storyText)
    }

    // --- Fallback Intelligent Generators ---

    private fun generateFallbackStory(
        idea: String,
        characters: String,
        location: String,
        mood: String,
        ending: String,
        language: String,
        style: String,
        actionType: String,
        currentStory: String
    ): String {
        val charName = if (characters.isNotBlank()) characters else "कबीर (एक निडर खोजकर्ता)"
        val locName = if (location.isNotBlank()) location else "हिमालय की बर्फीली रहस्यमयी वादियां"

        return when (language) {
            "Hindi" -> {
                when (actionType) {
                    "SHORTEN" -> """
                        ${charName} $locName के केंद्र में खड़ा था। सन्नाटे को चीरती हुई एक रहस्यमयी रोशनी चमकी। 
                        उसने बिना डरे कदम आगे बढ़ाया और प्राचीन रहस्य को छू लिया। एक क्षण में सब कुछ बदल गया और सच सामने आ गया!
                    """.trimIndent()

                    "EXPAND" -> """
                        रात का सन्नाटा गहरा चुका था और $locName पर जमी बर्फ में $charName की सांसों की भाप तैर रही थी।
                        $charName ने अपनी मुट्ठियां भींची। हवा में प्राचीन ऊर्जा की सिहरन थी।
                        अचानक पत्थरों के बीच से नीली रोशनी फूट पड़ी। जैसे ही उसने हाथ आगे बढ़ाया, हवा थम गई।
                        $mood के इस पल में उसने महसूस किया कि यह अंत नहीं, बल्कि एक नए युग की शुरुआत है।
                        $ending के साथ सच्चाई पूरे ब्रह्मांड के सामने उजागर हो गई!
                    """.trimIndent()

                    else -> """
                        दृश्य 1: $locName का मनोरम और रहस्यमयी वातावरण। हवा में एक अजीब सी खामोशी है।
                        
                        $charName धीरे-धीरे आगे बढ़ता है। उसके चेहरे पर $mood के भाव साफ नजर आ रहे हैं। सदियों से दफन एक राज आज जागने वाला था।
                        
                        अचानक धरती हल्की सी हिलती है और सामने एक अद्भुत अलौकिक दृश्य प्रकट होता है। 
                        
                        $charName ने कहा: "अगर आज मैंने यह कदम नहीं उठाया, तो इतिहास कभी माफ नहीं करेगा।"
                        
                        और फिर वही हुआ जिसका किसी को अंदेशा नहीं था—$ending ने सबको चौंका दिया!
                    """.trimIndent()
                }
            }

            "Urdu" -> {
                """
                    رات کی خاموشی میں $locName کا منظر انتہائی پُراسرار تھا۔
                    $charName نے آگے قدم بڑھایا، اس کے دل میں ایک انوکھا ولولہ اور $mood کی جھلک تھی۔
                    اچانک ایک تیز نور چمکا اور صدیوں پرانا راز افشاں ہو گیا۔
                    یہ سچائی نہ صرف حیران کن تھی بلکہ اس نے قسمت کے رخ کو بدل کر رکھ دیا!
                """.trimIndent()
            }

            else -> {
                when (actionType) {
                    "SHORTEN" -> """
                        Amidst the quiet ruins of $locName, $charName stepped forward. A sudden ethereal flash revealed the ancient relic. 
                        With decisive resolve, destiny shifted forever in a breathtaking climax!
                    """.trimIndent()

                    "EXPAND" -> """
                        The cold wind swept across $locName, carrying echoes of forgotten legends.
                        $charName stood at the precipice, eyes locked onto the ancient monolithic structure. 
                        Tension crackled through the air in a $mood crescendo as glowing glyphs illuminated the surrounding mist.
                        Reaching out with trembling resolve, the barrier dissolved.
                        In a final $ending, the true power of the universe was revealed.
                    """.trimIndent()

                    else -> """
                        Scene Hook: The breathtaking expanse of $locName enveloped in cinematic mist.
                        
                        $charName advances with determined footsteps, embodying an intense $mood atmosphere. 
                        
                        As ancient symbols begin to pulsate with vibrant neon luminescence, the stakes reach their peak.
                        
                        $charName whispers: "We only get one chance to rewrite tomorrow."
                        
                        In an exhilarating climax, $ending unfurls, leaving an unforgettable visual spectacle.
                    """.trimIndent()
                }
            }
        }
    }

    private fun generateFallbackScenes(
        project: ProjectEntity,
        storyText: String,
        numScenes: Int
    ): List<SceneEntity> {
        val list = mutableListOf<SceneEntity>()
        val durationPerScene = maxOf(5, (parseDurationSeconds(project.targetDuration) / numScenes))
        val isHindi = project.language == "Hindi"
        val isUrdu = project.language == "Urdu"

        val defaultShots = listOf(
            "Wide establishing drone shot, slow cinematic push-in",
            "Medium shot, low-angle dynamic track",
            "Intense close-up on character's eyes and micro-expression",
            "Over-the-shoulder tracking shot following character movement",
            "Dutch angle dramatic pan with volumetric lighting",
            "Slow-motion orbital 360 camera turn",
            "High-angle bird's eye view pulling back into the clouds",
            "Dynamic steadycam sprint tracking action sequence"
        )

        val defaultLightings = listOf(
            "Golden hour warm rim lighting with cinematic haze",
            "Neon cyberpunk reflections with deep moody shadows",
            "Dramatic god rays piercing through dense fog",
            "Soft diffused twilight glow with specular highlights",
            "Ethereal bioluminescent ambient glow"
        )

        for (i in 1..numScenes) {
            val sceneTitle = when (i) {
                1 -> if (isHindi) "शुरुआती हुक और परिचय" else "The Hook & Setting"
                2 -> if (isHindi) "रहस्य की खोज" else "The Discovery"
                3 -> if (isHindi) "तनाव और संघर्ष" else "Rising Tension"
                numScenes -> if (isHindi) "महा-चरमोत्कर्ष और अंत" else "The Grand Climax"
                else -> if (isHindi) "कहानी का महत्वपूर्ण मोड़ (दृश्य $i)" else "Crucial Beat (Scene $i)"
            }

            val desc = if (isHindi) {
                "दृश्य $i: ${project.characters.ifBlank { "मुख्य पात्र" }} $sceneTitle के दौरान स्थिति का सामना करता है।"
            } else if (isUrdu) {
                "منظر $i: مرکزی کردار پُراسرار حالات میں آگے بڑھتا ہے۔"
            } else {
                "Scene $i: ${project.characters.ifBlank { "The protagonist" }} confronts pivotal developments during $sceneTitle."
            }

            val dialogue = if (isHindi) {
                when (i) {
                    1 -> "सब कुछ शांत लग रहा है, पर खतरा करीब है।"
                    2 -> "यह वही संकेत है जिसका मुझे इंतजार था!"
                    numScenes -> "आज इतिहास हमेशा के लिए बदल जाएगा!"
                    else -> "हमें बिना रुके आगे बढ़ना होगा।"
                }
            } else if (isUrdu) {
                "وقت کم ہے اور منزل قریب!"
            } else {
                when (i) {
                    1 -> "Everything looks calm, but the storm is approaching."
                    2 -> "This is the exact sign I was searching for."
                    numScenes -> "History will be rewritten right here, right now!"
                    else -> "Keep moving forward, we can't stop now."
                }
            }

            val vo = if (isHindi) {
                "जब उम्मीदें खत्म होने लगी थीं, तभी एक नई राह खुल गई।"
            } else {
                "When all hope seemed lost, a new path revealed itself."
            }

            list.add(
                SceneEntity(
                    projectId = project.id,
                    sceneNumber = i,
                    sceneTitle = sceneTitle,
                    sceneDescription = desc,
                    characters = project.characters.ifBlank { "Main Protagonist" },
                    location = project.location.ifBlank { "Atmospheric Cinematic Set" },
                    characterActions = "Paces deliberately, glances around cautiously, then interacts with focal element.",
                    emotion = project.mood,
                    cameraShot = defaultShots[(i - 1) % defaultShots.size],
                    lighting = defaultLightings[(i - 1) % defaultLightings.size],
                    background = "Highly detailed textured environment with atmospheric depth particles.",
                    dialogue = dialogue,
                    voiceOver = vo,
                    duration = "${durationPerScene}s"
                )
            )
        }
        return list
    }

    private fun buildFallbackImagePrompt(
        project: ProjectEntity,
        scene: SceneEntity,
        charRef: String
    ): String {
        val charDetails = if (charRef.isNotBlank()) charRef else "${scene.characters}, distinct facial features, signature cinematic wardrobe"
        return """
            Cinematic 8K masterpiece photograph, ${project.style} visual style. 
            Character: $charDetails, expressing intense ${scene.emotion}, ${scene.characterActions}.
            Location: ${scene.location}, detailed architecture, ${scene.background}.
            Camera: ${scene.cameraShot}, 35mm anamorphic prime lens, shallow depth of field f/1.4, perfectly framed.
            Lighting: ${scene.lighting}, volumetric sun rays, soft rim light, deep contrast shadows.
            Atmosphere: Ultra-photorealistic texture, cinematic color grade, volumetric atmospheric haze, octane render quality, 8k resolution, Unreal Engine 5 aesthetic, photorealistic detail.
        """.trimIndent()
    }

    private fun buildFallbackVideoTextPrompt(
        project: ProjectEntity,
        scene: SceneEntity,
        charRef: String
    ): String {
        return """
            [Veo / AI Video Directorial Prompt]
            Scene ${scene.sceneNumber}: ${scene.sceneTitle} (Duration: ${scene.duration})
            Visual Style: ${project.style}
            Character Consistency: ${if (charRef.isNotBlank()) charRef else scene.characters}
            
            Action & Movement:
            ${scene.characterActions}. Fluid natural movements, realistic cloth physics and hair sway in the breeze.
            
            Camera Direction:
            ${scene.cameraShot}, smooth gimbal stabilization, cinematic pacing matching ${scene.duration}.
            
            Lighting & Environment:
            ${scene.lighting}. Dynamic atmospheric particles drifting in ${scene.location}.
            
            Audio & Dialogue:
            Spoken Dialogue: "${scene.dialogue}"
            Voice Emotion: ${scene.emotion}, natural mouth lip-sync alignment.
            SFX & Ambient Audio: Subtle ambient environmental soundscapes, immersive sub-bass swell.
            
            Continuity:
            Preserve exact facial geometry, costume wear, and scene spatial layout from preceding shots.
        """.trimIndent()
    }

    private fun buildFallbackVideoJson(
        project: ProjectEntity,
        scene: SceneEntity,
        charRef: String
    ): String {
        val json = JSONObject()
        json.put("scene", scene.sceneNumber)
        json.put("duration", scene.duration)
        json.put("characters", if (charRef.isNotBlank()) charRef else scene.characters)
        json.put("action", scene.characterActions)
        json.put("dialogue", scene.dialogue)
        json.put("emotion", scene.emotion)
        json.put("camera", scene.cameraShot)
        json.put("lighting", scene.lighting)
        json.put("environment", "${scene.location} - ${scene.background}")
        json.put("audio", "Cinematic orchestral ambient score with subtle sound effects")
        json.put("lip_sync", "Accurate Hindi/English phoneme sync to dialogue")
        json.put("visual_style", "${project.style} 8K Cinematic")
        json.put("continuity", "Maintain identical character attire, lighting tone, and props")
        return json.toString(2)
    }

    private fun buildFallbackVoiceScript(
        scenes: List<SceneEntity>,
        voiceStyle: String,
        language: String
    ): String {
        val builder = StringBuilder()
        builder.append("=== AI VOICE-OVER MASTER SCRIPT ===\n")
        builder.append("Style: $voiceStyle | Language: $language\n\n")

        for (s in scenes) {
            builder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            builder.append("🎬 Scene ${s.sceneNumber}: ${s.sceneTitle}\n")
            builder.append("🎙️ [Tone: $voiceStyle, ${s.emotion}]\n")
            builder.append("⏱️ [Pause: 1.2s]\n\n")
            if (s.voiceOver.isNotBlank()) {
                builder.append("Voice-Over: \"${s.voiceOver}\"\n")
            } else {
                builder.append("Voice-Over: \"${s.dialogue}\"\n")
            }
            builder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
        }
        return builder.toString().trim()
    }

    private fun buildFallbackYouTubePackage(
        project: ProjectEntity,
        storyText: String
    ): Map<String, String> {
        val baseName = project.name.ifBlank { "The Untold AI Story" }
        return mapOf(
            "title" to "🔥 $baseName | Incredible AI Short Film (4K)",
            "altTitles" to "1. What Happened in $baseName Will Shock You!\n2. The Secret of $baseName Revealed ⚡\n3. You Won't Believe This Ending! ($baseName)\n4. $baseName: Full AI Story in 60 Seconds\n5. The Mystery Nobody Saw Coming...",
            "description" to """
                Watch the complete cinematic AI story: $baseName.
                Created with cutting-edge AI video generation tools.
                
                📌 Highlights:
                0:00 - The Beginning
                0:20 - The Turning Point
                0:45 - The Unbelievable Climax
                
                🔔 Subscribe for more mind-blowing AI stories, Shorts, and video prompts daily!
                💬 What was your favorite moment? Comment below!
            """.trimIndent(),
            "hashtags" to "#Shorts #AIVideo #StoryTime #HindiStories #Cinematic #AIAnimation #ViralVideo #TrendingShorts #MyAIVideoStudio #YouTubeShorts",
            "tags" to "ai video, ai story, hindi kahani, shorts, viral shorts, artificial intelligence, veo video, ai prompt, short story, cinematic ai, trending, youtube shorts, reel, hindi short film",
            "shortCaption" to "This ending changed everything... 😱 Watch till the end! #Shorts #AIVideo",
            "thumbnailText" to "THE UNTOLD TRUTH! ⚠️",
            "pinnedComment" to "🔥 Rate this story from 1 to 10 in the comments! Should we make Part 2? 👇"
        )
    }

    private fun parseDurationSeconds(durationStr: String): Int {
        return when {
            durationStr.contains("30") -> 30
            durationStr.contains("60") -> 60
            durationStr.contains("90") -> 90
            else -> 60
        }
    }
}

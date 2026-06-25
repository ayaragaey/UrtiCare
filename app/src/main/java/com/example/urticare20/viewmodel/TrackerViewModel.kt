package com.example.urticare20.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.urticare20.data.ChatContextAssembler
import com.example.urticare20.data.TrackerDatabaseHelper
import com.example.urticare20.model.EntryType
import com.example.urticare20.model.LogEntry
import com.example.urticare20.model.ChatMessage
import com.example.urticare20.model.ChatSession
import com.example.urticare20.model.MenstruationCycle
import com.example.urticare20.model.ProfileAntihistamine
import com.example.urticare20.model.ProfileCortisone
import com.example.urticare20.model.ProfileOtherMedication
import com.example.urticare20.model.CompletedMedicationCourse
import com.example.urticare20.model.MedicationReminder
import com.example.urticare20.MedicationReminderReceiver
import com.example.urticare20.serializeReminders
import com.example.urticare20.deserializeReminders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.ZonedDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class CollisionWarning(
    val type: EntryType,
    val timestamp: String,
    val isEdit: Boolean,
    val entryId: String? = null
)

sealed class AppDeletedItem {
    data class SingleEntry(val entry: LogEntry) : AppDeletedItem()
    data class MultipleEntries(val entries: List<LogEntry>) : AppDeletedItem()
    data class Menstruation(val cycle: MenstruationCycle) : AppDeletedItem()
    data class Antihistamine(val med: ProfileAntihistamine) : AppDeletedItem()
    data class Cortisone(val med: ProfileCortisone) : AppDeletedItem()
    data class ChronicIllness(val name: String) : AppDeletedItem()
    data class ChatArchive(val session: ChatSession) : AppDeletedItem()
    data class OtherMed(val med: ProfileOtherMedication) : AppDeletedItem()
}

enum class MilestoneType { ADHERENCE, REMISSION }

data class MilestoneAlert(
    val id: String,
    val type: MilestoneType,
    val message: String,
    val borderHex: String
)

data class PendingStreakBreak(
    val type: EntryType,
    val timestamp: String,
    val metadata: String?,
    val streakText: String,
    val isAdherence: Boolean
)

class TrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = TrackerDatabaseHelper(application)
    private val contextAssembler = ChatContextAssembler(application)

    // Sentiment intercept state
    val pendingSentimentMessage = mutableStateOf<String?>(null)
    private val sharedPrefs = application.getSharedPreferences("urticare_prefs", Context.MODE_PRIVATE)

    // Flow exposing all entries
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    // Persistent fast-picking list for custom alternative medications
    private val _customAlternatives = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val customAlternatives: StateFlow<List<Pair<String, String>>> = _customAlternatives.asStateFlow()

    // Alert dialog State for duplicate temporal logging
    val collisionWarning = mutableStateOf<CollisionWarning?>(null)

    // Streak break interceptor state
    val pendingStreakBreak = mutableStateOf<PendingStreakBreak?>(null)

    // Last deleted item for app-wide undo options
    val lastDeletedItem = mutableStateOf<AppDeletedItem?>(null)

    // Navigated calendar date state for monthly rolling summary views
    val navigatedDate = mutableStateOf<ZonedDateTime>(ZonedDateTime.now())

    // Chatbot States
    private val _currentChat = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentChat: StateFlow<List<ChatMessage>> = _currentChat.asStateFlow()

    private val _archivedChats = MutableStateFlow<List<ChatSession>>(emptyList())
    val archivedChats: StateFlow<List<ChatSession>> = _archivedChats.asStateFlow()

    val isUrtiTyping = mutableStateOf(false)

    // Dynamic advice repository lists loaded from assets
    private val _anxietyAdvice = mutableListOf<String>()
    private val _stressAdvice = mutableListOf<String>()
    private val _burnoutAdvice = mutableListOf<String>()
    private val _pressureAdvice = mutableListOf<String>()

    // --- Profile & Menstruation Cycle State & Operations ---
    val profileName = MutableStateFlow(sharedPrefs.getString("profile_name", "") ?: "")
    val profileBirthDate = MutableStateFlow(sharedPrefs.getString("profile_birth_date", "") ?: "")
    val profileAge = MutableStateFlow(sharedPrefs.getString("profile_age", "") ?: "")
    val profileSex = MutableStateFlow(sharedPrefs.getString("profile_sex", "") ?: "")
    val profilePregnant = MutableStateFlow(sharedPrefs.getString("profile_pregnant", "") ?: "")
    val profilePregnancyMonth = MutableStateFlow(sharedPrefs.getString("profile_pregnancy_month", "") ?: "")
    val profileDiagnoses = MutableStateFlow<Set<String>>(sharedPrefs.getStringSet("profile_diagnoses", emptySet()) ?: emptySet())
    val profileCortisoneName = MutableStateFlow(sharedPrefs.getString("profile_cortisone_name", "") ?: "")
    val profileCortisoneMg = MutableStateFlow(sharedPrefs.getString("profile_cortisone_mg", "") ?: "")
    val profileOnXolair = MutableStateFlow(sharedPrefs.getBoolean("profile_on_xolair", false))

    private val _profileCortisones = MutableStateFlow<List<ProfileCortisone>>(emptyList())
    val profileCortisones: StateFlow<List<ProfileCortisone>> = _profileCortisones.asStateFlow()

    private val _profileOtherMedications = MutableStateFlow<List<ProfileOtherMedication>>(emptyList())
    val profileOtherMedications: StateFlow<List<ProfileOtherMedication>> = _profileOtherMedications.asStateFlow()

    private val _completedMedicationCourses = MutableStateFlow<List<CompletedMedicationCourse>>(emptyList())
    val completedMedicationCourses: StateFlow<List<CompletedMedicationCourse>> = _completedMedicationCourses.asStateFlow()

    private val _profileReminders = MutableStateFlow<List<MedicationReminder>>(emptyList())
    val profileReminders: StateFlow<List<MedicationReminder>> = _profileReminders.asStateFlow()

    private val _profileChronicIllnesses = MutableStateFlow<List<String>>(emptyList())
    val profileChronicIllnesses: StateFlow<List<String>> = _profileChronicIllnesses.asStateFlow()

    private val _menstruationCycles = MutableStateFlow<List<MenstruationCycle>>(emptyList())
    val menstruationCycles: StateFlow<List<MenstruationCycle>> = _menstruationCycles.asStateFlow()

    private val _profileAntihistamines = MutableStateFlow<List<ProfileAntihistamine>>(emptyList())
    val profileAntihistamines: StateFlow<List<ProfileAntihistamine>> = _profileAntihistamines.asStateFlow()

    // Milestone alerts queue
    private val _activeMilestoneAlerts = MutableStateFlow<List<MilestoneAlert>>(emptyList())
    val activeMilestoneAlerts: StateFlow<List<MilestoneAlert>> = _activeMilestoneAlerts.asStateFlow()

    // Streaks state flows
    private val _adherenceStreakHours = MutableStateFlow<Long?>(null)
    val adherenceStreakHours: StateFlow<Long?> = _adherenceStreakHours.asStateFlow()

    private val _remissionStreakDays = MutableStateFlow<Long?>(null)
    val remissionStreakDays: StateFlow<Long?> = _remissionStreakDays.asStateFlow()

    init {
        loadEntries()
        loadCustomAlternatives()
        loadChatData()
        loadUrtiKnowledge()
        loadMenstruationCycles()
        loadProfileAntihistamines()
        loadProfileCortisones()
        loadProfileOtherMedications()
        loadCompletedMedicationCourses()
        loadProfileReminders()
        loadProfileChronicIllnesses()

        // Periodic milestone evaluation tick (every 10 seconds to check real-time boundary transitions)
        viewModelScope.launch {
            while (true) {
                evaluateMilestones()
                delay(10000)
            }
        }
    }

    fun saveProfile(
        name: String,
        birthDate: String,
        age: String,
        sex: String,
        pregnant: String,
        month: String,
        diagnoses: Set<String>,
        cortisoneName: String,
        cortisoneMg: String,
        onXolair: Boolean
    ) {
        profileName.value = name
        profileBirthDate.value = birthDate
        profileAge.value = age
        profileSex.value = sex
        profilePregnant.value = pregnant
        profilePregnancyMonth.value = month
        profileDiagnoses.value = diagnoses
        profileCortisoneName.value = cortisoneName
        profileCortisoneMg.value = cortisoneMg
        profileOnXolair.value = onXolair

        sharedPrefs.edit()
            .putString("profile_name", name)
            .putString("profile_birth_date", birthDate)
            .putString("profile_age", age)
            .putString("profile_sex", sex)
            .putString("profile_pregnant", pregnant)
            .putString("profile_pregnancy_month", month)
            .putStringSet("profile_diagnoses", diagnoses)
            .putString("profile_cortisone_name", cortisoneName)
            .putString("profile_cortisone_mg", cortisoneMg)
            .putBoolean("profile_on_xolair", onXolair)
            .apply()
        
        cleanUpFlareUpMetadata()
    }

    fun cleanUpFlareUpMetadata() {
        val currentEntries = _entries.value.toMutableList()
        var updatedAny = false

        for (i in currentEntries.indices) {
            val entry = currentEntries[i]
            if (entry.type == EntryType.FLARE_UP) {
                var finalMetadata = entry.metadata
                val apd = "Potentially caused by Autoimmune Progesterone Dermatitis (APD)"
                val pregText = "Possibly a pregnancy-associated flare-up"
                var modified = false

                // Check APD
                if (!finalMetadata.isNullOrEmpty() && finalMetadata.contains(apd)) {
                    if (!isDateInMenstruationCycle(entry.timestamp)) {
                        finalMetadata = finalMetadata.split("; ")
                            .filter { it != apd }
                            .joinToString("; ")
                        modified = true
                    }
                }

                // Check Pregnancy
                if (!finalMetadata.isNullOrEmpty() && finalMetadata.contains(pregText)) {
                    val stillPregnant = (profileSex.value == "F" && profilePregnant.value == "Yes")
                    if (!stillPregnant) {
                        finalMetadata = finalMetadata.split("; ")
                            .filter { it != pregText }
                            .joinToString("; ")
                        modified = true
                    }
                }

                if (modified) {
                    val cleanedMetadata = if (finalMetadata.isNullOrEmpty() || finalMetadata.trim().isEmpty()) null else finalMetadata.trim()
                    dbHelper.updateEntryDetails(entry.id, entry.timestamp, cleanedMetadata)
                    currentEntries[i] = entry.copy(metadata = cleanedMetadata)
                    updatedAny = true
                }
            }
        }

        if (updatedAny) {
            _entries.value = currentEntries
        }
    }

    private fun loadMenstruationCycles() {
        val json = sharedPrefs.getString("menstruation_cycles", "") ?: ""
        _menstruationCycles.value = deserializeCycles(json)
    }

    private fun saveMenstruationCycles(cycles: List<MenstruationCycle>) {
        _menstruationCycles.value = cycles
        val json = serializeCycles(cycles)
        sharedPrefs.edit().putString("menstruation_cycles", json).apply()
        cleanUpFlareUpMetadata()
    }

    fun addMenstruationCycle(startDate: String, endDate: String? = null) {
        val current = _menstruationCycles.value.toMutableList()
        val newCycle = MenstruationCycle(
            id = UUID.randomUUID().toString(),
            startDate = startDate,
            endDate = endDate
        )
        current.add(newCycle)
        saveMenstruationCycles(current.sortedByDescending { it.startDate })
    }

    fun updateMenstruationCycle(id: String, startDate: String, endDate: String?) {
        val current = _menstruationCycles.value.map {
            if (it.id == id) it.copy(startDate = startDate, endDate = endDate) else it
        }
        saveMenstruationCycles(current.sortedByDescending { it.startDate })
    }

    fun deleteMenstruationCycle(id: String) {
        val cycle = _menstruationCycles.value.find { it.id == id }
        if (cycle != null) {
            val item = AppDeletedItem.Menstruation(cycle)
            lastDeletedItem.value = item
            viewModelScope.launch {
                delay(6000)
                if (lastDeletedItem.value == item) {
                    lastDeletedItem.value = null
                }
            }
        }
        val current = _menstruationCycles.value.filter { it.id != id }
        saveMenstruationCycles(current)
    }

    private fun serializeCycles(cycles: List<MenstruationCycle>): String {
        val array = JSONArray()
        cycles.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("startDate", it.startDate)
            if (it.endDate != null) {
                obj.put("endDate", it.endDate)
            } else {
                obj.put("endDate", JSONObject.NULL)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeCycles(json: String): List<MenstruationCycle> {
        if (json.isEmpty()) return emptyList()
        val list = mutableListOf<MenstruationCycle>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val endDateVal = if (obj.isNull("endDate")) null else obj.getString("endDate")
                list.add(
                    MenstruationCycle(
                        id = obj.getString("id"),
                        startDate = obj.getString("startDate"),
                        endDate = endDateVal
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun isDateInMenstruationCycle(timestamp: String): Boolean {
        val logDate = try {
            ZonedDateTime.parse(timestamp).toLocalDate()
        } catch (e: Exception) {
            try {
                LocalDate.parse(timestamp.substring(0, 10))
            } catch (ex: Exception) {
                return false
            }
        }
        for (cycle in _menstruationCycles.value) {
            val start = try { LocalDate.parse(cycle.startDate) } catch (e: Exception) { continue }
            if (logDate.isBefore(start)) continue
            if (cycle.endDate == null) {
                // Ongoing cycle is active from start onwards
                return true
            } else {
                val end = try { LocalDate.parse(cycle.endDate) } catch (e: Exception) { continue }
                if (!logDate.isAfter(end)) {
                    return true
                }
            }
        }
        return false
    }

    fun getActiveOtherMedicationsOnDate(timestamp: String): List<String> {
        val targetDate = try {
            ZonedDateTime.parse(timestamp).withZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDate()
        } catch (e: Exception) {
            try {
                LocalDate.parse(timestamp.substring(0, 10))
            } catch (ex: Exception) {
                return emptyList()
            }
        }

        val activeMeds = mutableListOf<String>()

        // Check active profile medications
        _profileOtherMedications.value.forEach { med ->
            if (med.startDate.isNotEmpty()) {
                try {
                    val medStart = LocalDate.parse(med.startDate)
                    if (!targetDate.isBefore(medStart)) {
                        activeMeds.add("${med.name} (${med.mgs} mg)")
                    }
                } catch (e: Exception) {
                    // Ignore parsing error
                }
            }
        }

        // Check completed courses
        _completedMedicationCourses.value.forEach { course ->
            if (course.startDate.isNotEmpty() && course.endDate.isNotEmpty()) {
                try {
                    val start = LocalDate.parse(course.startDate)
                    val end = LocalDate.parse(course.endDate)
                    if (!targetDate.isBefore(start) && !targetDate.isAfter(end)) {
                        activeMeds.add("${course.name} (${course.mgs} mg)")
                    }
                } catch (e: Exception) {
                    // Ignore parsing error
                }
            }
        }

        return activeMeds.distinct()
    }

    private fun adjustAntihistamineMetadata(timestamp: String, currentMetadata: String?, excludeId: String? = null): String? {
        val safeMetadata = currentMetadata ?: ""
        
        // 1. Split parts and filter out existing "Ongoing Medication:"
        val parts = safeMetadata.split(":::")
            .filter { !it.startsWith("Ongoing Medication:") && it.isNotEmpty() }
            .toMutableList()
            
        // 2. Check if this is a short interval (< 8 hours) relative to the preceding antihistamine
        var hasShortInterval = false
        try {
            val targetTime = ZonedDateTime.parse(timestamp)
            val otherAntihistamines = dbHelper.getAllEntries()
                .filter { it.type == EntryType.ANTIHISTAMINE && it.id != excludeId }
            
            val precedingEntry = otherAntihistamines
                .mapNotNull { entry ->
                    try {
                        val entryTime = ZonedDateTime.parse(entry.timestamp)
                        if (entryTime.isBefore(targetTime)) entry to entryTime else null
                    } catch (e: Exception) {
                        null
                    }
                }
                .maxByOrNull { it.second }
                
            if (precedingEntry != null) {
                val diffMins = Math.abs(Duration.between(targetTime, precedingEntry.second).toMinutes())
                hasShortInterval = diffMins < 8 * 60
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        if (hasShortInterval) {
            val activeMeds = getActiveOtherMedicationsOnDate(timestamp)
            if (activeMeds.isNotEmpty()) {
                parts.add("Ongoing Medication: ${activeMeds.joinToString(", ")}")
            }
        }
        
        val joined = parts.joinToString(":::")
        return if (joined.isEmpty()) null else joined
    }


    private fun loadProfileAntihistamines() {
        val json = sharedPrefs.getString("profile_antihistamines", "") ?: ""
        _profileAntihistamines.value = deserializeAntihistamines(json)
    }

    private fun saveProfileAntihistamines(list: List<ProfileAntihistamine>) {
        _profileAntihistamines.value = list
        val json = serializeAntihistamines(list)
        sharedPrefs.edit().putString("profile_antihistamines", json).apply()
    }

    fun addProfileAntihistamine(name: String, mgs: String, generation: String, isMain: Boolean = false) {
        val current = _profileAntihistamines.value.toMutableList()
        if (isMain) {
            for (i in 0 until current.size) {
                current[i] = current[i].copy(isMain = false)
            }
        }
        val newAntihistamine = ProfileAntihistamine(
            id = UUID.randomUUID().toString(),
            name = name,
            mgs = mgs,
            generation = generation,
            isMain = isMain
        )
        current.add(newAntihistamine)
        saveProfileAntihistamines(current)
    }

    fun updateProfileAntihistamine(id: String, name: String, mgs: String, generation: String, isMain: Boolean) {
        val current = _profileAntihistamines.value.map {
            if (it.id == id) {
                it.copy(name = name, mgs = mgs, generation = generation, isMain = isMain)
            } else {
                if (isMain) it.copy(isMain = false) else it
            }
        }
        saveProfileAntihistamines(current)
    }

    fun toggleMainAntihistamine(id: String) {
        val current = _profileAntihistamines.value.map {
            if (it.id == id) {
                it.copy(isMain = !it.isMain)
            } else {
                it.copy(isMain = false)
            }
        }
        saveProfileAntihistamines(current)
    }

    fun deleteProfileAntihistamine(id: String) {
        val med = _profileAntihistamines.value.find { it.id == id }
        if (med != null) {
            val item = AppDeletedItem.Antihistamine(med)
            lastDeletedItem.value = item
            viewModelScope.launch {
                delay(6000)
                if (lastDeletedItem.value == item) {
                    lastDeletedItem.value = null
                }
            }
        }
        val current = _profileAntihistamines.value.filter { it.id != id }
        saveProfileAntihistamines(current)
    }

    fun detectAntihistamineGeneration(medName: String, onResult: (String) -> Unit) {
        val name = medName.trim().lowercase()
        if (name.isEmpty()) {
            onResult("")
            return
        }

        // 1. Local Dictionary Check
        val gen1Keywords = listOf(
            "diphenhydramine", "benadryl",
            "hydroxyzine", "atarax", "vistaril",
            "chlorpheniramine", "chlorphenamine", "chlor-trimeton",
            "promethazine", "phenergan",
            "cyproheptadine", "doxepin",
            "doxylamine", "unisom",
            "carbinoxamine", "clemastine", "tavist",
            "tripelennamine", "brompheniramine", "dimetapp",
            "pheniramine", "triprolidine", "dexchlorpheniramine", "polaramine"
        )
        val gen2Keywords = listOf(
            "cetirizine", "zyrtec", "reactine",
            "levocetirizine", "xyzal",
            "loratadine", "claritin", "clarytin",
            "desloratadine", "clarinex", "aerius",
            "fexofenadine", "allegra", "telfast",
            "bilastine", "bilaxten", "blexten",
            "rupatadine", "rupafin",
            "ebastine", "ebastel",
            "mizolastine", "mizollen",
            "acrivastine", "semprex",
            "ketotifen", "zaditen",
            "azelastine", "astelin", "astepro",
            "olopatadine", "patanol", "pataday"
        )

        for (k in gen1Keywords) {
            if (name.contains(k)) {
                onResult("1st")
                return
            }
        }
        for (k in gen2Keywords) {
            if (name.contains(k)) {
                onResult("2nd")
                return
            }
        }

        // 2. Web Search Fallback (using Wikipedia API)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val formattedName = medName.trim().replace(" ", "_")
                val urlString = "https://en.wikipedia.org/w/api.php?action=query&prop=extracts&exintro&explaintext&titles=$formattedName&format=json&redirects=1"
                val url = java.net.URL(urlString)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val lowerResponse = response.lowercase()
                
                val detectedGen = when {
                    lowerResponse.contains("first-generation antihistamine") || 
                    lowerResponse.contains("first generation antihistamine") || 
                    lowerResponse.contains("1st-generation antihistamine") || 
                    lowerResponse.contains("1st generation antihistamine") || 
                    lowerResponse.contains("sedating antihistamine") -> "1st"
                    
                    lowerResponse.contains("second-generation antihistamine") || 
                    lowerResponse.contains("second generation antihistamine") || 
                    lowerResponse.contains("2nd-generation antihistamine") || 
                    lowerResponse.contains("2nd generation antihistamine") || 
                    lowerResponse.contains("non-sedating antihistamine") -> "2nd"
                    
                    else -> ""
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(detectedGen)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult("")
                }
            }
        }
    }

    private fun serializeAntihistamines(list: List<ProfileAntihistamine>): String {
        val array = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("mgs", it.mgs)
            obj.put("generation", it.generation)
            obj.put("isMain", it.isMain)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeAntihistamines(json: String): List<ProfileAntihistamine> {
        if (json.isEmpty()) return emptyList()
        val list = mutableListOf<ProfileAntihistamine>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val isMainVal = if (obj.has("isMain")) obj.getBoolean("isMain") else false
                list.add(
                    ProfileAntihistamine(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        mgs = obj.getString("mgs"),
                        generation = obj.getString("generation"),
                        isMain = isMainVal
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun loadEntries() {
        viewModelScope.launch {
            val list = dbHelper.getAllEntries()
            _entries.value = list
            evaluateMilestones()
            
            try {
                val file = java.io.File("C:\\Users\\pc\\.gemini\\antigravity\\brain\\aa6702dc-4b71-4544-b311-1159c894563d\\db_dump.txt")
                val sb = StringBuilder()
                sb.append("--- DATABASE ENTRIES DUMP ---\n")
                list.forEach { entry ->
                    sb.append("ID: ${entry.id} | Timestamp: ${entry.timestamp} | Type: ${entry.type} | Metadata: ${entry.metadata}\n")
                }
                sb.append("\n--- ACTIVE OTHER MEDS ---\n")
                _profileOtherMedications.value.forEach { med ->
                    sb.append("ID: ${med.id} | Name: ${med.name} | Mgs: ${med.mgs} | StartDate: ${med.startDate}\n")
                }
                sb.append("\n--- COMPLETED COURSES ---\n")
                _completedMedicationCourses.value.forEach { c ->
                    sb.append("ID: ${c.id} | Name: ${c.name} | Mgs: ${c.mgs} | StartDate: ${c.startDate} | EndDate: ${c.endDate}\n")
                }
                file.writeText(sb.toString())
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun dismissMilestoneAlert(id: String) {
        _activeMilestoneAlerts.value = _activeMilestoneAlerts.value.filter { it.id != id }
    }

    fun evaluateMilestones() {
        val currentEntries = _entries.value
        val now = ZonedDateTime.now()
        
        // Calculate current streaks using pure evaluator helper
        val adherenceHours = com.example.urticare20.util.MilestoneEvaluator.calculateAdherenceHours(currentEntries, now)
        val remissionDays = com.example.urticare20.util.MilestoneEvaluator.calculateRemissionDays(currentEntries, now)
        
        _adherenceStreakHours.value = adherenceHours
        _remissionStreakDays.value = remissionDays

        // 1. Adherence Milestones (+48 hours medication-free windows)
        if (adherenceHours != null) {
            val lastMedId = com.example.urticare20.util.MilestoneEvaluator.getAdherenceLogId(currentEntries)
            val celebratedMedId = sharedPrefs.getString("milestone_adherence_med_id", "") ?: ""
            val celebratedHours = sharedPrefs.getLong("milestone_adherence_hours", 0L)
            
            val currentTier = (adherenceHours / 48).toInt()
            var evaluatedCelebratedHours = celebratedHours

            if (lastMedId != celebratedMedId) {
                // New streak started, reset celebrated hours for this streak
                evaluatedCelebratedHours = 0L
                sharedPrefs.edit()
                    .putString("milestone_adherence_med_id", lastMedId)
                    .putLong("milestone_adherence_hours", 0L)
                    .apply()
            }
            
            if (currentTier > 0) {
                val targetCelebrationHours = currentTier * 48L
                if (targetCelebrationHours > evaluatedCelebratedHours) {
                    val newAlerts = mutableListOf<MilestoneAlert>()
                    for (t in 1..currentTier) {
                        val tierHours = t * 48L
                        if (tierHours > evaluatedCelebratedHours) {
                            val msg = com.example.urticare20.util.MilestoneEvaluator.getAdherenceSentence(t)
                            newAlerts.add(
                                MilestoneAlert(
                                    id = UUID.randomUUID().toString(),
                                    type = MilestoneType.ADHERENCE,
                                    message = msg,
                                    borderHex = "#99DDFF"
                                )
                            )
                        }
                    }
                    if (newAlerts.isNotEmpty()) {
                        _activeMilestoneAlerts.value = _activeMilestoneAlerts.value + newAlerts
                        sharedPrefs.edit()
                            .putLong("milestone_adherence_hours", targetCelebrationHours)
                            .apply()
                    }
                }
            }
        } else {
            // Reset streak persistence if no meds logged
            sharedPrefs.edit()
                .putString("milestone_adherence_med_id", "none")
                .putLong("milestone_adherence_hours", 0L)
                .apply()
        }

        // 2. Remission Milestones (+1 week flare-up free windows)
        if (remissionDays != null) {
            val lastFlareId = com.example.urticare20.util.MilestoneEvaluator.getRemissionLogId(currentEntries)
            val celebratedFlareId = sharedPrefs.getString("milestone_remission_flare_id", "") ?: ""
            val celebratedWeeks = sharedPrefs.getInt("milestone_remission_weeks", 0)
            
            val remissionHours = com.example.urticare20.util.MilestoneEvaluator.calculateRemissionHours(currentEntries, now) ?: 0L
            val currentTier = (remissionHours / 168).toInt() // 168 hours = 1 week
            var evaluatedCelebratedWeeks = celebratedWeeks

            if (lastFlareId != celebratedFlareId) {
                // New streak started, reset celebrated weeks for this streak
                evaluatedCelebratedWeeks = 0
                sharedPrefs.edit()
                    .putString("milestone_remission_flare_id", lastFlareId)
                    .putInt("milestone_remission_weeks", 0)
                    .apply()
            }
            
            if (currentTier > 0) {
                if (currentTier > evaluatedCelebratedWeeks) {
                    val newAlerts = mutableListOf<MilestoneAlert>()
                    for (t in 1..currentTier) {
                        if (t > evaluatedCelebratedWeeks) {
                            val msg = com.example.urticare20.util.MilestoneEvaluator.getRemissionSentence(t)
                            newAlerts.add(
                                MilestoneAlert(
                                    id = UUID.randomUUID().toString(),
                                    type = MilestoneType.REMISSION,
                                    message = msg,
                                    borderHex = "#FFB3B3"
                                )
                            )
                        }
                    }
                    if (newAlerts.isNotEmpty()) {
                        _activeMilestoneAlerts.value = _activeMilestoneAlerts.value + newAlerts
                        sharedPrefs.edit()
                            .putInt("milestone_remission_weeks", currentTier)
                            .apply()
                    }
                }
            }
        } else {
            // Reset streak persistence if no flares logged
            sharedPrefs.edit()
                .putString("milestone_remission_flare_id", "none")
                .putInt("milestone_remission_weeks", 0)
                .apply()
        }
    }

    // --- SharedPreferences Persistence for Alternatives ---

    private fun loadCustomAlternatives() {
        val raw = sharedPrefs.getStringSet("alternatives", emptySet()) ?: emptySet()
        _customAlternatives.value = raw.mapNotNull {
            val parts = it.split(":::")
            if (parts.size == 2) parts[0] to parts[1] else null
        }.sortedBy { it.first }
    }

    fun saveCustomAlternative(name: String, mg: String) {
        val current = _customAlternatives.value.toMutableList()
        val entry = name.trim() to mg.trim()
        if (entry.first.isNotEmpty() && !current.contains(entry)) {
            current.add(entry)
            _customAlternatives.value = current.sortedBy { it.first }
            
            val rawSet = current.map { "${it.first}:::${it.second}" }.toSet()
            sharedPrefs.edit().putStringSet("alternatives", rawSet).apply()
        }
    }

    // --- Core Operations ---

    /**
     * Checks if there's an entry of the same type within the exact same minute YYYY-MM-DDTHH:mm
     */
    private fun checkMinuteCollision(type: EntryType, timestamp: String, excludeId: String? = null): Boolean {
        try {
            val targetMin = timestamp.substring(0, 16) // "YYYY-MM-DDTHH:mm"
            return _entries.value.any { e ->
                if (e.id == excludeId) return@any false
                if (e.type != type) return@any false
                val entryMin = e.timestamp.substring(0, 16)
                entryMin == targetMin
            }
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Triggers tracking event with collision checks, metadata support, and auto-merge fallback.
     */
    fun addEntry(type: EntryType, timestamp: String, metadata: String? = null, force: Boolean = false): Boolean {
        // 1. Check duplicate minute overlap
        if (!force && checkMinuteCollision(type, timestamp)) {
            collisionWarning.value = CollisionWarning(type, timestamp, isEdit = false)
            return false
        }

        // 2. Check if this breaks a milestone streak (if not forced)
        if (!force) {
            if (type == EntryType.FLARE_UP) {
                val remissionDays = _remissionStreakDays.value
                if (remissionDays != null && remissionDays >= 7) {
                    if (metadata == null || !metadata.contains("Streak Break Reason:")) {
                        pendingStreakBreak.value = PendingStreakBreak(
                            type = type,
                            timestamp = timestamp,
                            metadata = metadata,
                            streakText = "consecutive flare-up free streak of $remissionDays days",
                            isAdherence = false
                        )
                        return false
                    }
                }
            } else if (type == EntryType.ANTIHISTAMINE || type == EntryType.CORTISONE || type == EntryType.ALTERNATIVE) {
                val adherenceHours = _adherenceStreakHours.value
                if (adherenceHours != null && adherenceHours >= 48) {
                    if (metadata == null || !metadata.contains("Streak Break Reason:")) {
                        val days = adherenceHours / 24
                        val hoursStr = if (days > 0) "$days days ${adherenceHours % 24} hours" else "$adherenceHours hours"
                        pendingStreakBreak.value = PendingStreakBreak(
                            type = type,
                            timestamp = timestamp,
                            metadata = metadata,
                            streakText = "consecutive medication-free streak of $hoursStr",
                            isAdherence = true
                        )
                        return false
                    }
                }
            }
        }

        // 2. Clear same-minute duplicates if auto-merge is active
        if (force) {
            val targetMin = timestamp.substring(0, 16)
            _entries.value.forEach { e ->
                if (e.type == type && e.timestamp.substring(0, 16) == targetMin) {
                    dbHelper.deleteEntry(e.id)
                }
            }
        }

        // 3. Auto-detect menstruation cycle match for APD
        var finalMetadata = metadata
        if (type == EntryType.FLARE_UP && isDateInMenstruationCycle(timestamp)) {
            val apd = "Potentially caused by Autoimmune Progesterone Dermatitis (APD)"
            if (finalMetadata.isNullOrEmpty()) {
                finalMetadata = apd
            } else if (!finalMetadata.contains(apd)) {
                finalMetadata = "$finalMetadata; $apd"
            }
        }

        // 3.5. Auto-detect ongoing other medications for Flare-up
        if (type == EntryType.FLARE_UP) {
            val activeMeds = getActiveOtherMedicationsOnDate(timestamp)
            if (activeMeds.isNotEmpty()) {
                val ongoingText = "Ongoing Medication: ${activeMeds.joinToString(", ")}"
                if (finalMetadata.isNullOrEmpty()) {
                    finalMetadata = ongoingText
                } else if (!finalMetadata.contains("Ongoing Medication:")) {
                    finalMetadata = "$finalMetadata; $ongoingText"
                }
            }
        }

        if (type == EntryType.FLARE_UP && profileSex.value == "F" && profilePregnant.value == "Yes") {
            val pregText = "Possibly a pregnancy-associated flare-up"
            if (finalMetadata.isNullOrEmpty()) {
                finalMetadata = pregText
            } else if (!finalMetadata.contains(pregText)) {
                finalMetadata = "$finalMetadata; $pregText"
            }
        }

        if (type == EntryType.ANTIHISTAMINE) {
            finalMetadata = adjustAntihistamineMetadata(timestamp, finalMetadata)
        }

        // 4. Insert and reload
        val newEntry = LogEntry(
            id = "${type.name}_${UUID.randomUUID()}",
            timestamp = timestamp,
            type = type,
            metadata = finalMetadata
        )
        val success = dbHelper.insertEntry(newEntry)
        loadEntries()
        return success
    }

    fun updateEntryDetails(id: String, newTimestamp: String, newMetadata: String?, force: Boolean = false): Boolean {
        val targetEntry = _entries.value.find { it.id == id } ?: return false
        if (!force && checkMinuteCollision(targetEntry.type, newTimestamp, id)) {
            collisionWarning.value = CollisionWarning(targetEntry.type, newTimestamp, isEdit = true, entryId = id)
            return false
        }
        if (force) {
            val targetMin = newTimestamp.substring(0, 16)
            _entries.value.forEach { e ->
                if (e.id != id && e.type == targetEntry.type && e.timestamp.substring(0, 16) == targetMin) {
                    dbHelper.deleteEntry(e.id)
                }
            }
        }

        // Manage APD metadata auto-write/auto-clean on update
        var finalMetadata = newMetadata
        val apd = "Potentially caused by Autoimmune Progesterone Dermatitis (APD)"
        if (targetEntry.type == EntryType.FLARE_UP) {
            // APD auto-write/auto-clean
            if (isDateInMenstruationCycle(newTimestamp)) {
                if (finalMetadata.isNullOrEmpty()) {
                    finalMetadata = apd
                } else if (!finalMetadata.contains(apd)) {
                    finalMetadata = "$finalMetadata; $apd"
                }
            } else {
                if (!finalMetadata.isNullOrEmpty() && finalMetadata.contains(apd)) {
                    finalMetadata = finalMetadata.split("; ")
                        .filter { it != apd }
                        .joinToString("; ")
                    if (finalMetadata.isEmpty()) finalMetadata = null
                }
            }

            // Ongoing other medications auto-write/auto-clean
            // 1. Remove existing "Ongoing Medication:" metadata if present
            if (!finalMetadata.isNullOrEmpty()) {
                finalMetadata = finalMetadata.split("; ")
                    .filter { !it.startsWith("Ongoing Medication:") }
                    .joinToString("; ")
                if (finalMetadata.isEmpty()) finalMetadata = null
            }
            // 2. Add current active other medications for the updated timestamp
            val activeMeds = getActiveOtherMedicationsOnDate(newTimestamp)
            if (activeMeds.isNotEmpty()) {
                val ongoingText = "Ongoing Medication: ${activeMeds.joinToString(", ")}"
                if (finalMetadata.isNullOrEmpty()) {
                    finalMetadata = ongoingText
                } else {
                    finalMetadata = "$finalMetadata; $ongoingText"
                }
            }

            // Pregnancy auto-write/auto-clean
            val pregText = "Possibly a pregnancy-associated flare-up"
            if (profileSex.value == "F" && profilePregnant.value == "Yes") {
                if (finalMetadata.isNullOrEmpty()) {
                    finalMetadata = pregText
                } else if (!finalMetadata.contains(pregText)) {
                    finalMetadata = "$finalMetadata; $pregText"
                }
            } else {
                if (!finalMetadata.isNullOrEmpty() && finalMetadata.contains(pregText)) {
                    finalMetadata = finalMetadata.split("; ")
                        .filter { it != pregText }
                        .joinToString("; ")
                    if (finalMetadata.isEmpty()) finalMetadata = null
                }
            }
        } else if (targetEntry.type == EntryType.ANTIHISTAMINE) {
            finalMetadata = adjustAntihistamineMetadata(newTimestamp, finalMetadata, excludeId = id)
        }

        val success = dbHelper.updateEntryDetails(id, newTimestamp, finalMetadata)
        loadEntries()
        return success
    }

    /**
     * Retrospectively modifies a timestamp with minute-level collision guards.
     */
    fun updateEntry(id: String, newTimestamp: String, force: Boolean = false): Boolean {
        val targetEntry = _entries.value.find { it.id == id } ?: return false
        return updateEntryDetails(id, newTimestamp, targetEntry.metadata, force)
    }

    /**
     * Deletes/Clears a record.
     */
    fun deleteEntry(id: String) {
        val entry = _entries.value.find { it.id == id }
        if (entry != null) {
            val item = AppDeletedItem.SingleEntry(entry)
            lastDeletedItem.value = item
            viewModelScope.launch {
                delay(6000) // Clear the undo option after 6 seconds
                if (lastDeletedItem.value == item) {
                    lastDeletedItem.value = null
                }
            }
        }
        dbHelper.deleteEntry(id)
        loadEntries()
    }

    /**
     * Restores the last deleted entry.
     */
    fun undoDelete() {
        val deleted = lastDeletedItem.value ?: return
        viewModelScope.launch {
            when (deleted) {
                is AppDeletedItem.SingleEntry -> {
                    dbHelper.insertEntry(deleted.entry)
                    loadEntries()
                }
                is AppDeletedItem.MultipleEntries -> {
                    deleted.entries.forEach { dbHelper.insertEntry(it) }
                    loadEntries()
                }
                is AppDeletedItem.Menstruation -> {
                    val current = _menstruationCycles.value.toMutableList()
                    current.add(deleted.cycle)
                    saveMenstruationCycles(current.sortedByDescending { it.startDate })
                }
                is AppDeletedItem.Antihistamine -> {
                    val current = _profileAntihistamines.value.toMutableList()
                    current.add(deleted.med)
                    saveProfileAntihistamines(current)
                }
                is AppDeletedItem.Cortisone -> {
                    val current = _profileCortisones.value.toMutableList()
                    current.add(deleted.med)
                    saveProfileCortisones(current)
                }
                is AppDeletedItem.OtherMed -> {
                    val current = _profileOtherMedications.value.toMutableList()
                    current.add(deleted.med)
                    saveProfileOtherMedications(current)
                }
                is AppDeletedItem.ChronicIllness -> {
                    addProfileChronicIllness(deleted.name)
                }
                is AppDeletedItem.ChatArchive -> {
                    val current = _archivedChats.value.toMutableList()
                    _archivedChats.value = listOf(deleted.session) + current
                    saveChatData()
                }
            }
            lastDeletedItem.value = null
        }
    }

    /**
     * Resets active alerts.
     */
    fun dismissCollision() {
        collisionWarning.value = null
    }

    /**
     * Category-specific resets, allowing Pill, Flare Up, or Xolair logs to be cleared independently.
     */
    fun clearAllEntriesOfTypes(types: List<EntryType>) {
        val entriesToClear = _entries.value.filter { it.type in types }
        if (entriesToClear.isNotEmpty()) {
            val item = AppDeletedItem.MultipleEntries(entriesToClear)
            lastDeletedItem.value = item
            viewModelScope.launch {
                delay(6000)
                if (lastDeletedItem.value == item) {
                    lastDeletedItem.value = null
                }
            }
        }
        viewModelScope.launch {
            dbHelper.clearAllEntriesOfTypes(types)
            loadEntries()
        }
    }

    /**
     * Restores zero state tracker parameters entirely.
     */
    fun clearAllEntries() {
        dbHelper.clearAllEntries()
        loadEntries()
    }

    // --- Analytics Lookbacks ---

    /**
     * Computes distinct occurrences of a type within the sliding lookback window of X hours relative to now.
     */
    fun getMovingWindowCount(type: EntryType, hours: Long): Int {
        val now = ZonedDateTime.now()
        return _entries.value.count { entry ->
            if (entry.type != type) return@count false
            try {
                val entryTime = ZonedDateTime.parse(entry.timestamp)
                val duration = Duration.between(entryTime, now)
                val seconds = duration.seconds
                seconds in 0..(hours * 3600)
            } catch (e: Exception) {
                false
            }
        }
    }

    // --- Rolling Calendar Month Paginations ---

    fun previousMonth() {
        navigatedDate.value = navigatedDate.value.minusMonths(1)
    }

    fun nextMonth() {
        navigatedDate.value = navigatedDate.value.plusMonths(1)
    }

    /**
     * Computes monthly aggregate totals matching navigated dates.
     */
    fun getMonthlyCounts(type: EntryType): Int {
        val targetYear = navigatedDate.value.year
        val targetMonthValue = navigatedDate.value.monthValue
        
        return _entries.value.count { entry ->
            if (entry.type != type) return@count false
            try {
                val entryTime = ZonedDateTime.parse(entry.timestamp)
                entryTime.year == targetYear && entryTime.monthValue == targetMonthValue
            } catch (e: Exception) {
                false
            }
        }
    }

    // --- Urti Chatbot Persistence & Operations ---

    private fun loadChatData() {
        val currentJson = sharedPrefs.getString("current_chat", "") ?: ""
        _currentChat.value = deserializeMessages(currentJson)

        val archivesJson = sharedPrefs.getString("archived_chats", "") ?: ""
        _archivedChats.value = deserializeSessions(archivesJson)
    }

    private fun saveChatData() {
        val currentJson = serializeMessages(_currentChat.value)
        val archivesJson = serializeSessions(_archivedChats.value)

        sharedPrefs.edit()
            .putString("current_chat", currentJson)
            .putString("archived_chats", archivesJson)
            .apply()
    }

    /**
     * Starts a new chat session. If clearPrevious is false, archives the current session.
     */
    fun startNewChat(clearPrevious: Boolean) {
        val current = _currentChat.value
        val hasUserMessages = current.any { it.sender == "User" }

        if (!clearPrevious && hasUserMessages) {
            // Archive current session
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            val titleDate = try {
                val firstMsgTimestamp = current.firstOrNull()?.timestamp ?: ZonedDateTime.now().toString()
                ZonedDateTime.parse(firstMsgTimestamp).format(formatter)
            } catch (e: Exception) {
                ZonedDateTime.now().format(formatter)
            }

            val newArchive = ChatSession(
                id = UUID.randomUUID().toString(),
                title = titleDate,
                startTime = ZonedDateTime.now().toString(),
                messages = current
            )
            _archivedChats.value = listOf(newArchive) + _archivedChats.value
        }

        // Initialize new chat with Urti's greeting message
        _currentChat.value = listOf(
            ChatMessage(
                sender = "Urti",
                text = "Hi! Urti is here for you and this is your safe space to pause, vent, or simply breathe.",
                timestamp = ZonedDateTime.now().toString()
            )
        )
        saveChatData()
    }

    /**
     * Resumes the last active chat session.
     */
    fun resumeLastChat() {
        val current = _currentChat.value.toMutableList()
        if (current.isEmpty()) {
            // Initialize if completely blank
            current.add(
                ChatMessage(
                    sender = "Urti",
                    text = "Hi! Urti is here for you and this is your safe space to pause, vent, or simply breathe.",
                    timestamp = ZonedDateTime.now().toString()
                )
            )
        } else {
            // Append continuation greeting
            current.add(
                ChatMessage(
                    sender = "Urti",
                    text = "Hi again! How are things feeling right now?",
                    timestamp = ZonedDateTime.now().toString()
                )
            )
        }
        _currentChat.value = current
        saveChatData()
    }

    /**
     * Resumes a specific archived chat session.
     */
    fun resumeArchive(sessionId: String) {
        val archives = _archivedChats.value.toMutableList()
        val index = archives.indexOfFirst { it.id == sessionId }
        if (index != -1) {
            val session = archives[index]
            val current = session.messages.toMutableList()
            current.add(
                ChatMessage(
                    sender = "Urti",
                    text = "Hi again! How are things feeling right now?",
                    timestamp = ZonedDateTime.now().toString()
                )
            )
            _currentChat.value = current
            
            // Remove from archives as it's now the active chat
            archives.removeAt(index)
            _archivedChats.value = archives
            saveChatData()
        }
    }

    fun renameArchive(sessionId: String, newTitle: String) {
        _archivedChats.value = _archivedChats.value.map {
            if (it.id == sessionId) it.copy(title = newTitle.trim()) else it
        }
        saveChatData()
    }

    fun deleteArchive(sessionId: String) {
        val session = _archivedChats.value.find { it.id == sessionId }
        if (session != null) {
            val item = AppDeletedItem.ChatArchive(session)
            lastDeletedItem.value = item
            viewModelScope.launch {
                delay(6000)
                if (lastDeletedItem.value == item) {
                    lastDeletedItem.value = null
                }
            }
        }
        _archivedChats.value = _archivedChats.value.filter { it.id != sessionId }
        saveChatData()
    }

    fun clearCurrentChat() {
        _currentChat.value = emptyList()
        saveChatData()
    }

    /**
     * Checks if user message contains any physical/emotional stress keywords.
     */
    fun containsStressSentiment(text: String): Boolean {
        val lowerText = text.lowercase()
        val stressKeywords = listOf(
            // Physical
            "burning", "itching", "hives", "welts", "stinging",
            // Emotional
            "anxious", "panicking", "can't sleep", "overwhelmed", "exhausted"
        )
        return stressKeywords.any { lowerText.contains(it) }
    }

    /**
     * Sends a user message and triggers Urti's empathetic typing responses.
     */
    fun sendUserMessage(text: String) {
        if (text.trim().isEmpty()) return

        val current = _currentChat.value.toMutableList()
        val userMsg = ChatMessage(
            sender = "User",
            text = text.trim(),
            timestamp = ZonedDateTime.now().toString()
        )
        current.add(userMsg)
        _currentChat.value = current
        saveChatData()

        if (containsStressSentiment(text)) {
            // Save as pending and wait for breathing completion intercept
            pendingSentimentMessage.value = text.trim()
        } else {
            // Normal message submission triggers response immediately
            triggerUrtiResponseFlow(text.trim())
        }
    }

    fun completeSentimentBreathing() {
        val text = pendingSentimentMessage.value
        if (text != null) {
            pendingSentimentMessage.value = null
            triggerUrtiResponseFlow(text)
        }
    }

    private fun triggerUrtiResponseFlow(userMsgText: String) {
        viewModelScope.launch {
            isUrtiTyping.value = true
            delay(1200) // Emulate typing delay
            isUrtiTyping.value = false

            val replyText = getUrtiResponse(userMsgText)
            val urtiMsg = ChatMessage(
                sender = "Urti",
                text = replyText,
                timestamp = ZonedDateTime.now().toString()
            )
            
            val updated = _currentChat.value.toMutableList()
            updated.add(urtiMsg)
            _currentChat.value = updated
            saveChatData()
        }
    }

    private fun loadUrtiKnowledge() {
        try {
            val context = getApplication<Application>().applicationContext
            context.assets.open("urti_knowledge.txt").bufferedReader().use { reader ->
                var currentCategory = ""
                reader.forEachLine { line ->
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) return@forEachLine
                    if (trimmed.startsWith("[")) {
                        currentCategory = trimmed.uppercase()
                    } else if (trimmed.startsWith("-")) {
                        val advice = trimmed.removePrefix("-").trim()
                        when {
                            currentCategory.contains("ANXIETY") -> _anxietyAdvice.add(advice)
                            currentCategory.contains("STRESS") -> _stressAdvice.add(advice)
                            currentCategory.contains("BURNOUT") -> _burnoutAdvice.add(advice)
                            currentCategory.contains("PRESSURE") -> _pressureAdvice.add(advice)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Safety checks to ensure we never have empty lists
        if (_anxietyAdvice.isEmpty()) {
            _anxietyAdvice.add("Practice Square-Breathing: Inhale for 4 seconds, hold for 4, exhale for 4, and rest at the bottom for 4.")
        }
        if (_stressAdvice.isEmpty()) {
            _stressAdvice.add("Drop Your Shoulders: Intentionally unclamp your jaw, lower your shoulders, and relax your tongue.")
        }
        if (_burnoutAdvice.isEmpty()) {
            _burnoutAdvice.add("Initiate an Emergency Pause: Stop running. Give yourself non-negotiable permission to rest.")
        }
        if (_pressureAdvice.isEmpty()) {
            _pressureAdvice.add("Enforce an Explicit Pause: When high pressure hits, take a deliberate timeout to lower your heart rate.")
        }
    }

    private fun getUrtiResponse(userText: String): String {
        // Compile the comprehensive 3-Layer context block dynamically
        val contextBlock = contextAssembler.assembleContextBlock(_currentChat.value)
        val text = userText.lowercase()

        // 1. Safety Bound Intercept check
        val isSafetySwelling = text.contains("throat") || text.contains("swelling") || 
                               text.contains("tongue") || text.contains("breathing") || 
                               text.contains("tightness") || text.contains("choking")
        if (isSafetySwelling) {
            return "I hear how frightening this is, but since you mentioned swelling or difficulty breathing, this could be a severe, life-threatening allergic reaction (anaphylaxis). Please seek immediate professional medical care or call emergency services right now. Your physical safety is the absolute first priority. Urti is holding space for you, but please contact emergency services immediately."
        }

        // 2. Compliance Alerts check (Unsafe window < 8 hours)
        val hasMedicationIntervalWarning = contextBlock.contains("CRITICAL MEDICATION ALERT")
        val complianceWarningText = if (hasMedicationIntervalWarning) {
            "\n\n*Note from Urti: I noticed your recent medication entries were logged less than 8 hours apart. Please be gentle with your body and make sure you are following safe dosing protocols.*"
        } else {
            ""
        }

        // 3. Activity Stream Analysis
        val last4EntriesDescription = if (!contextBlock.contains("Fallback Active")) {
            val lastLog = dbHelper.getAllEntries().firstOrNull()
            if (lastLog != null) {
                val actionDesc = when (lastLog.type) {
                    EntryType.FLARE_UP -> "I noticed you logged a Flare-up recently."
                    EntryType.ANTIHISTAMINE -> "I noticed you logged taking an Antihistamine recently."
                    EntryType.CORTISONE -> "I see you logged a Cortisone dose recently."
                    EntryType.XOLAIR_150 -> "I see you logged your Xolair 150 mg injection recently."
                    EntryType.XOLAIR_300 -> "I see you logged your Xolair 300 mg injection recently."
                    EntryType.ALTERNATIVE -> "I noticed you logged alternative medication (${lastLog.metadata ?: "Unnamed"}) recently."
                }
                "$actionDesc Let's focus on calming down the body and relaxing the nervous system."
            } else {
                ""
            }
        } else {
            ""
        }

        // 4. Sentiment parsing and advice selection
        val responseBody = when {
            text.contains("anxiety") || text.contains("anxious") || text.contains("panic") || text.contains("scared") || text.contains("fear") -> {
                val advice = if (_anxietyAdvice.isNotEmpty()) _anxietyAdvice.random() else "Take a slow breath..."
                "I hear you. Anxiety acts like a chemical fuse that commands immune cells to dump histamine, triggering painful hives and itching. Take a moment to anchor yourself with this advice:\n\n\"$advice\"\n\n$last4EntriesDescription"
            }
            text.contains("stress") || text.contains("stressed") || text.contains("exhaust") || text.contains("heavy") || text.contains("chaos") -> {
                val advice = if (_stressAdvice.isNotEmpty()) _stressAdvice.random() else "Relax your shoulders and jaw."
                "I hear how much stress you are carrying right now. Emotional distress literally commands mast cells to degranulate and release histamines. Let's practice down-regulating your nervous system:\n\n\"$advice\"\n\n$last4EntriesDescription"
            }
            text.contains("burnout") || text.contains("burned out") || text.contains("empty") || text.contains("depleted") || text.contains("tired") -> {
                val advice = if (_burnoutAdvice.isNotEmpty()) _burnoutAdvice.random() else "Give yourself absolute permission to rest."
                "You sound completely empty. Chronic hives and recovery from burnout require non-negotiable permission to pause. Urti's recommendation:\n\n\"$advice\"\n\n$last4EntriesDescription"
            }
            text.contains("pressure") || text.contains("pressured") || text.contains("work") || text.contains("deadline") || text.contains("busy") -> {
                val advice = if (_pressureAdvice.isNotEmpty()) _pressureAdvice.random() else "Take a deliberate timeout."
                "The intense pressure you're under locks up your chest and nerves. Step back from the pressure loop for a moment:\n\n\"$advice\"\n\n$last4EntriesDescription"
            }
            text.contains("thank") || text.contains("thanks") || text.contains("helpful") || text.contains("good") || text.contains("happy") || text.contains("better") -> {
                listOf(
                    "I'm so glad to hear that! Keeping our nervous system balanced is a beautiful way to protect your skin and body. How else can I support your balance today?",
                    "That brings absolute peace to my heart. Supporting your well-being is my ultimate mission. Carry this quiet, grounded space with you.",
                    "Wonderful. We are taking beautiful steps together toward peace, rest, and strength."
                ).random()
            }
            else -> {
                val allAdvice = _anxietyAdvice + _stressAdvice + _burnoutAdvice + _pressureAdvice
                val advice = if (allAdvice.isNotEmpty()) allAdvice.random() else "Take a slow breath..."
                listOf(
                    "Thank you for sharing that with Urti. Your feelings are fully valid. Let's take a pause together. How is your body feeling in this exact moment?\n\n\"$advice\"",
                    "I am listening. Sometimes just venting it out helps release the tension stored in our shoulders and jaw. Urti's guidance for you:\n\n\"$advice\"\n\n$last4EntriesDescription",
                    "That sounds like a lot to hold today. Remember to be gentle with yourself. You are allowed to be a work in progress and a masterpiece at the same time."
                ).random()
            }
        }

        return "$responseBody$complianceWarningText"
    }

    // --- JSON Serialization Helpers ---

    private fun serializeMessages(messages: List<ChatMessage>): String {
        val array = JSONArray()
        messages.forEach {
            val obj = JSONObject()
            obj.put("sender", it.sender)
            obj.put("text", it.text)
            obj.put("timestamp", it.timestamp)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeMessages(json: String): List<ChatMessage> {
        if (json.isEmpty()) return emptyList()
        val list = mutableListOf<ChatMessage>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChatMessage(
                        sender = obj.getString("sender"),
                        text = obj.getString("text"),
                        timestamp = obj.getString("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun serializeSessions(sessions: List<ChatSession>): String {
        val array = JSONArray()
        sessions.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            obj.put("startTime", it.startTime)
            
            val msgArray = JSONArray()
            it.messages.forEach { m ->
                val mObj = JSONObject()
                mObj.put("sender", m.sender)
                mObj.put("text", m.text)
                mObj.put("timestamp", m.timestamp)
                msgArray.put(mObj)
            }
            obj.put("messages", msgArray)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeSessions(json: String): List<ChatSession> {
        if (json.isEmpty()) return emptyList()
        val list = mutableListOf<ChatSession>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val msgList = mutableListOf<ChatMessage>()
                val msgArray = obj.getJSONArray("messages")
                for (j in 0 until msgArray.length()) {
                    val mObj = msgArray.getJSONObject(j)
                    msgList.add(
                        ChatMessage(
                            sender = mObj.getString("sender"),
                            text = mObj.getString("text"),
                            timestamp = mObj.getString("timestamp")
                        )
                    )
                }
                list.add(
                    ChatSession(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        startTime = obj.getString("startTime"),
                        messages = msgList
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun loadProfileCortisones() {
        val json = sharedPrefs.getString("profile_cortisones", "") ?: ""
        _profileCortisones.value = deserializeCortisones(json)
    }

    private fun saveProfileCortisones(list: List<ProfileCortisone>) {
        _profileCortisones.value = list
        val json = serializeCortisones(list)
        sharedPrefs.edit().putString("profile_cortisones", json).apply()
    }

    fun addProfileCortisone(name: String, mgs: String) {
        val current = _profileCortisones.value.toMutableList()
        val newCortisone = ProfileCortisone(
            id = UUID.randomUUID().toString(),
            name = name,
            mgs = mgs
        )
        current.add(newCortisone)
        saveProfileCortisones(current)
    }

    fun deleteProfileCortisone(id: String) {
        val med = _profileCortisones.value.find { it.id == id }
        if (med != null) {
            val item = AppDeletedItem.Cortisone(med)
            lastDeletedItem.value = item
            viewModelScope.launch {
                delay(6000)
                if (lastDeletedItem.value == item) {
                    lastDeletedItem.value = null
                }
            }
        }
        val current = _profileCortisones.value.filter { it.id != id }
        saveProfileCortisones(current)
    }

    fun updateProfileCortisone(id: String, name: String, mgs: String) {
        val current = _profileCortisones.value.map {
            if (it.id == id) it.copy(name = name, mgs = mgs) else it
        }
        saveProfileCortisones(current)
    }

    private fun serializeCortisones(list: List<ProfileCortisone>): String {
        val array = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("mgs", it.mgs)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeCortisones(json: String): List<ProfileCortisone> {
        if (json.isEmpty()) return emptyList()
        val list = mutableListOf<ProfileCortisone>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ProfileCortisone(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        mgs = obj.getString("mgs")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun loadProfileChronicIllnesses() {
        val raw = sharedPrefs.getString("profile_chronic_illnesses", "") ?: ""
        if (raw.isEmpty()) {
            _profileChronicIllnesses.value = emptyList()
        } else {
            _profileChronicIllnesses.value = raw.split(";;;").filter { it.isNotEmpty() }
        }
    }

    fun addProfileChronicIllness(name: String) {
        val current = _profileChronicIllnesses.value.toMutableList()
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && !current.contains(trimmed)) {
            current.add(trimmed)
            _profileChronicIllnesses.value = current
            sharedPrefs.edit().putString("profile_chronic_illnesses", current.joinToString(";;;")).apply()
        }
    }

    fun deleteProfileChronicIllness(name: String) {
        if (_profileChronicIllnesses.value.contains(name)) {
            val item = AppDeletedItem.ChronicIllness(name)
            lastDeletedItem.value = item
            viewModelScope.launch {
                delay(6000)
                if (lastDeletedItem.value == item) {
                    lastDeletedItem.value = null
                }
            }
        }
        val current = _profileChronicIllnesses.value.toMutableList()
        current.remove(name)
        _profileChronicIllnesses.value = current
        sharedPrefs.edit().putString("profile_chronic_illnesses", current.joinToString(";;;")).apply()
    }

    private fun loadProfileOtherMedications() {
        val json = sharedPrefs.getString("profile_other_medications", "") ?: ""
        _profileOtherMedications.value = deserializeOtherMedications(json)
    }

    private fun saveProfileOtherMedications(list: List<ProfileOtherMedication>) {
        _profileOtherMedications.value = list
        val json = serializeOtherMedications(list)
        sharedPrefs.edit().putString("profile_other_medications", json).apply()
    }

    fun addProfileOtherMedication(name: String, mgs: String, condition: String, frequency: String, medType: String, startDate: String) {
        val current = _profileOtherMedications.value.toMutableList()
        val newMed = ProfileOtherMedication(
            id = UUID.randomUUID().toString(),
            name = name,
            mgs = mgs,
            condition = condition,
            frequency = frequency,
            medType = medType,
            startDate = startDate
        )
        current.add(newMed)
        saveProfileOtherMedications(current)
    }

    fun deleteProfileOtherMedication(id: String) {
        val med = _profileOtherMedications.value.find { it.id == id }
        if (med != null) {
            val item = AppDeletedItem.OtherMed(med)
            lastDeletedItem.value = item
            viewModelScope.launch {
                delay(6000)
                if (lastDeletedItem.value == item) {
                    lastDeletedItem.value = null
                }
            }
        }
        val current = _profileOtherMedications.value.filter { it.id != id }
        saveProfileOtherMedications(current)
    }

    fun updateProfileOtherMedication(id: String, name: String, mgs: String, condition: String, frequency: String, medType: String, startDate: String) {
        val current = _profileOtherMedications.value.map {
            if (it.id == id) it.copy(name = name, mgs = mgs, condition = condition, frequency = frequency, medType = medType, startDate = startDate) else it
        }
        saveProfileOtherMedications(current)
    }

    private fun serializeOtherMedications(list: List<ProfileOtherMedication>): String {
        val array = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("mgs", it.mgs)
            obj.put("condition", it.condition)
            obj.put("frequency", it.frequency)
            obj.put("medType", it.medType)
            obj.put("startDate", it.startDate)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeOtherMedications(json: String): List<ProfileOtherMedication> {
        if (json.isEmpty()) return emptyList()
        val list = mutableListOf<ProfileOtherMedication>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ProfileOtherMedication(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        mgs = obj.getString("mgs"),
                        condition = if (obj.has("condition")) obj.getString("condition") else "",
                        frequency = if (obj.has("frequency")) obj.getString("frequency") else "",
                        medType = if (obj.has("medType")) obj.getString("medType") else "",
                        startDate = if (obj.has("startDate") && obj.getString("startDate").isNotEmpty()) obj.getString("startDate") else LocalDate.now().toString()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun loadProfileReminders() {
        val json = sharedPrefs.getString("profile_reminders", "") ?: ""
        _profileReminders.value = deserializeReminders(json)
    }

    private fun saveProfileReminders(list: List<MedicationReminder>) {
        _profileReminders.value = list
        val json = serializeReminders(list)
        sharedPrefs.edit().putString("profile_reminders", json).apply()
    }

    fun addProfileReminder(reminder: MedicationReminder, context: Context) {
        val current = _profileReminders.value.toMutableList()
        current.add(reminder)
        saveProfileReminders(current)
        if (reminder.isEnabled) {
            MedicationReminderReceiver.scheduleNextAlarm(context, reminder)
        }
    }

    fun deleteProfileReminder(id: String, context: Context) {
        MedicationReminderReceiver.cancelAlarm(context, id)
        val current = _profileReminders.value.filter { it.id != id }
        saveProfileReminders(current)
    }

    fun toggleProfileReminder(id: String, isEnabled: Boolean, context: Context) {
        val current = _profileReminders.value.map {
            if (it.id == id) {
                val updated = it.copy(isEnabled = isEnabled)
                if (isEnabled) {
                    MedicationReminderReceiver.scheduleNextAlarm(context, updated)
                } else {
                    MedicationReminderReceiver.cancelAlarm(context, id)
                }
                updated
            } else it
        }
        saveProfileReminders(current)
    }

    fun logCompletedMedicationCourse(
        medId: String,
        startDate: String,
        endDate: String,
        durationDays: Int
    ) {
        val med = _profileOtherMedications.value.find { it.id == medId } ?: return
        
        // Remove from active other medications
        deleteProfileOtherMedication(medId)
        
        // Add to completed courses history
        val current = _completedMedicationCourses.value.toMutableList()
        val newCourse = CompletedMedicationCourse(
            id = UUID.randomUUID().toString(),
            name = med.name,
            mgs = med.mgs,
            condition = med.condition,
            frequency = med.frequency,
            startDate = startDate,
            endDate = endDate,
            durationDays = durationDays,
            medType = med.medType
        )
        current.add(0, newCourse)
        saveCompletedMedicationCourses(current)
    }

    fun deleteCompletedMedicationCourse(id: String) {
        val current = _completedMedicationCourses.value.filter { it.id != id }
        saveCompletedMedicationCourses(current)
    }

    private fun loadCompletedMedicationCourses() {
        val json = sharedPrefs.getString("completed_medication_courses", "") ?: ""
        _completedMedicationCourses.value = deserializeCompletedCourses(json)
    }

    private fun saveCompletedMedicationCourses(list: List<CompletedMedicationCourse>) {
        _completedMedicationCourses.value = list
        val json = serializeCompletedCourses(list)
        sharedPrefs.edit().putString("completed_medication_courses", json).apply()
    }

    private fun serializeCompletedCourses(list: List<CompletedMedicationCourse>): String {
        val array = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("mgs", it.mgs)
            obj.put("condition", it.condition)
            obj.put("frequency", it.frequency)
            obj.put("startDate", it.startDate)
            obj.put("endDate", it.endDate)
            obj.put("durationDays", it.durationDays)
            obj.put("medType", it.medType)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeCompletedCourses(json: String): List<CompletedMedicationCourse> {
        if (json.isEmpty()) return emptyList()
        val list = mutableListOf<CompletedMedicationCourse>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CompletedMedicationCourse(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        mgs = obj.getString("mgs"),
                        condition = if (obj.has("condition")) obj.getString("condition") else "",
                        frequency = if (obj.has("frequency")) obj.getString("frequency") else "",
                        startDate = obj.getString("startDate"),
                        endDate = obj.getString("endDate"),
                        durationDays = obj.getInt("durationDays"),
                        medType = if (obj.has("medType")) obj.getString("medType") else ""
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}


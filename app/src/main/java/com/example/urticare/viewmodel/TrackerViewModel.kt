package com.example.urticare.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.urticare.data.ChatContextAssembler
import com.example.urticare.data.TrackerDatabaseHelper
import com.example.urticare.model.EntryType
import com.example.urticare.model.LogEntry
import com.example.urticare.model.ConsumableItem
import com.example.urticare.model.ChatMessage
import com.example.urticare.model.ChatSession
import com.example.urticare.model.MenstruationCycle
import com.example.urticare.model.ProfileAntihistamine
import com.example.urticare.model.ProfileCortisone
import com.example.urticare.model.ProfileOtherMedication
import com.example.urticare.model.CompletedMedicationCourse
import com.example.urticare.model.MedicationReminder
import com.example.urticare.MedicationReminderReceiver
import com.example.urticare.serializeReminders
import com.example.urticare.deserializeReminders
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
    data class Allergy(val name: String) : AppDeletedItem()
}

enum class MilestoneType { ADHERENCE, REMISSION }

data class MilestoneAlert(
    val id: String,
    val type: MilestoneType,
    val message: String,
    val borderHex: String,
    val durationText: String
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

    val currentChatLanguage = MutableStateFlow("EN")

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
    val profileBiologicalMedication = MutableStateFlow(sharedPrefs.getString("profile_biological_medication", "") ?: "")
    val profileBiologicalMg = MutableStateFlow(sharedPrefs.getString("profile_biological_mg", "") ?: "")

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

    private val _profileKnownAllergies = MutableStateFlow<List<String>>(emptyList())
    val profileKnownAllergies: StateFlow<List<String>> = _profileKnownAllergies.asStateFlow()


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

    // --- Consumables Catalog State & Operations ---
    private val _recentlyUsedConsumables = MutableStateFlow<List<String>>(emptyList())
    val recentlyUsedConsumables: StateFlow<List<String>> = _recentlyUsedConsumables.asStateFlow()

    private val _favoriteConsumableIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteConsumableIds: StateFlow<Set<String>> = _favoriteConsumableIds.asStateFlow()

    private val _favoriteConsumableNames = MutableStateFlow<List<String>>(emptyList())
    val favoriteConsumableNames: StateFlow<List<String>> = _favoriteConsumableNames.asStateFlow()

    init {
        dbHelper.seedConsumablesCatalogIfEmpty(application)
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
        loadProfileKnownAllergies()


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
        onXolair: Boolean,
        biologicalMedication: String = "",
        biologicalMg: String = ""
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
        profileBiologicalMedication.value = biologicalMedication
        profileBiologicalMg.value = biologicalMg

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
            .putString("profile_biological_medication", biologicalMedication)
            .putString("profile_biological_mg", biologicalMg)
            .apply()
        
        cleanUpFlareUpMetadata()
    }

    fun isProfileComplete(): Boolean {
        val hasName = profileName.value.trim().isNotEmpty()
        val hasAge = profileAge.value.trim().isNotEmpty() || profileBirthDate.value.trim().isNotEmpty()
        val hasGender = profileSex.value.trim().isNotEmpty()
        return hasName && hasAge && hasGender
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
            loadConsumablesState()
        }
    }

    fun dismissMilestoneAlert(id: String) {
        _activeMilestoneAlerts.value = _activeMilestoneAlerts.value.filter { it.id != id }
    }

    fun evaluateMilestones() {
        val currentEntries = _entries.value
        val now = ZonedDateTime.now()
        
        // Calculate current streaks using pure evaluator helper
        val adherenceHours = com.example.urticare.util.MilestoneEvaluator.calculateAdherenceHours(currentEntries, now)
        val remissionDays = com.example.urticare.util.MilestoneEvaluator.calculateRemissionDays(currentEntries, now)
        
        _adherenceStreakHours.value = adherenceHours
        _remissionStreakDays.value = remissionDays

        // 1. Adherence Milestones (+48 hours medication-free windows)
        if (adherenceHours != null) {
            val lastMedId = com.example.urticare.util.MilestoneEvaluator.getAdherenceLogId(currentEntries)
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
                            val msg = com.example.urticare.util.MilestoneEvaluator.getAdherenceSentence(t)
                            val durationText = if (t == 1) "48 Hours Stability" else "${t * 2} Days Stability"
                            newAlerts.add(
                                MilestoneAlert(
                                    id = UUID.randomUUID().toString(),
                                    type = MilestoneType.ADHERENCE,
                                    message = msg,
                                    borderHex = "#99DDFF",
                                    durationText = durationText
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
            val lastFlareId = com.example.urticare.util.MilestoneEvaluator.getRemissionLogId(currentEntries)
            val celebratedFlareId = sharedPrefs.getString("milestone_remission_flare_id", "") ?: ""
            val celebratedWeeks = sharedPrefs.getInt("milestone_remission_weeks", 0)
            
            val remissionHours = com.example.urticare.util.MilestoneEvaluator.calculateRemissionHours(currentEntries, now) ?: 0L
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
                            val msg = com.example.urticare.util.MilestoneEvaluator.getRemissionSentence(t)
                            val durationText = if (t == 1) "1 Week Remission" else "$t Weeks Remission"
                            newAlerts.add(
                                MilestoneAlert(
                                    id = UUID.randomUUID().toString(),
                                    type = MilestoneType.REMISSION,
                                    message = msg,
                                    borderHex = "#FFB3B3",
                                    durationText = durationText
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
        if (type == EntryType.CONSUMPTION) return false
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

        // 2. Clear same-minute duplicates if auto-merge is active (only for non-consumption entries)
        if (force && type != EntryType.CONSUMPTION) {
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
        if (force && targetEntry.type != EntryType.CONSUMPTION) {
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
     * Toggles trigger status for a consumption log entry.
     */
    fun toggleConsumptionTriggerStatus(id: String): Boolean {
        val entry = _entries.value.find { it.id == id && it.type == EntryType.CONSUMPTION } ?: return false
        val parts = entry.metadata?.split(":::") ?: return false
        if (parts.size >= 5 && parts[0] == "Consumption") {
            val status = if (parts.size >= 6) parts[5] else "Logged"
            val newStatus = if (status == "Trigger") "Logged" else "Trigger"
            val paddedParts = MutableList(6) { "" }
            parts.forEachIndexed { idx, part ->
                if (idx < 6) paddedParts[idx] = part
            }
            paddedParts[5] = newStatus
            val newMetadata = paddedParts.joinToString(":::")
            val success = dbHelper.updateEntryDetails(id, entry.timestamp, newMetadata)
            loadEntries()
            return success
        }
        return false
    }

    /**
     * Undoes the last N consumption log entries.
     */
    fun undoLastConsumptionLogs(count: Int) {
        val consumptionEntries = dbHelper.getAllEntries()
            .filter { it.type == EntryType.CONSUMPTION }
            .sortedByDescending { it.timestamp }
        val toDelete = consumptionEntries.take(count)
        toDelete.forEach {
            dbHelper.deleteEntry(it.id)
        }
        loadEntries()
    }

    fun deleteEntries(ids: List<String>) {
        ids.forEach { id ->
            dbHelper.deleteEntry(id)
        }
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
                is AppDeletedItem.Allergy -> {
                    addProfileKnownAllergy(deleted.name)
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

        currentChatLanguage.value = sharedPrefs.getString("current_chat_lang", "EN") ?: "EN"
    }

    private fun saveChatData() {
        val currentJson = serializeMessages(_currentChat.value)
        val archivesJson = serializeSessions(_archivedChats.value)

        sharedPrefs.edit()
            .putString("current_chat", currentJson)
            .putString("archived_chats", archivesJson)
            .putString("current_chat_lang", currentChatLanguage.value)
            .apply()
    }

    fun getUrtiMainGreeting(lang: String): String {
        return when (lang) {
            "AR" -> "مرحباً! أورتي هنا من أجلك وهذا هو مساحتك الآمنة للتوقف، التنفيس، أو ببساطة التنفس."
            "FR" -> "Salut! Urti est là pour vous et c'est votre espace de sécurité pour faire une pause, vous exprimer ou simplement respirer."
            "ES" -> "¡Hola! Urti está aquí para ti y este es tu espacio seguro para hacer una pausa, desahogarte o simplemente respirar."
            "IT" -> "Ciao! Urti è qui per te e questo è il tuo spazio sicuro per fare una pausa, sfogarti o semplicemente respirare."
            "DE" -> "Hallo! Urti ist für dich da und dies ist dein sicherer Ort, um innezuhalten, dich auszusprechen oder einfach durchzuatmen."
            "NL" -> "Hoi! Urti is er voor je en dit is jouw veilige plek om te pauzeren, je hart te luchten of gewoon te ademen."
            else -> "Hi! Urti is here for you and this is your safe space to pause, vent, or simply breathe."
        }
    }

    fun getUrtiMainContinuation(lang: String): String {
        return when (lang) {
            "AR" -> "أهلاً بك مجدداً! كيف تشعر الآن؟"
            "FR" -> "Re-bonjour! Comment vous sentez-vous en ce moment?"
            "ES" -> "¡Hola de nuevo! ¿Cómo te sientes en este momento?"
            "IT" -> "Ciao ancora! Come ti senti in questo momento?"
            "DE" -> "Hallo nochmal! Wie fühlst du dich gerade?"
            "NL" -> "Hallo alweer! Hoe voel je je op dit moment?"
            else -> "Hi again! How are things feeling right now?"
        }
    }

    /**
     * Starts a new chat session. If clearPrevious is false, archives the current session.
     */
    fun startNewChat(clearPrevious: Boolean, language: String) {
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
                messages = current,
                language = currentChatLanguage.value
            )
            _archivedChats.value = listOf(newArchive) + _archivedChats.value
        }

        // Set the new language
        currentChatLanguage.value = language

        // Initialize new chat with Urti's greeting message
        _currentChat.value = listOf(
            ChatMessage(
                sender = "Urti",
                text = getUrtiMainGreeting(language),
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
        val language = currentChatLanguage.value
        if (current.isEmpty()) {
            // Initialize if completely blank
            current.add(
                ChatMessage(
                    sender = "Urti",
                    text = getUrtiMainGreeting(language),
                    timestamp = ZonedDateTime.now().toString()
                )
            )
        } else {
            // Append continuation greeting
            current.add(
                ChatMessage(
                    sender = "Urti",
                    text = getUrtiMainContinuation(language),
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
            
            // Set current chat language to the archived session's language
            currentChatLanguage.value = session.language
            
            val current = session.messages.toMutableList()
            current.add(
                ChatMessage(
                    sender = "Urti",
                    text = getUrtiMainContinuation(session.language),
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
        val lang = currentChatLanguage.value
        val stressKeywords = when (lang) {
            "AR" -> listOf("حرق", "حكة", "شرى", "بثور", "لسع", "قلق", "هلع", "لا أستطيع النوم", "متعب", "منهك")
            "FR" -> listOf("brûlure", "démangeaison", "urticaire", "plaques", "piqûre", "anxieux", "panique", "peux pas dormir", "submergé", "épuisé")
            "ES" -> listOf("quemazón", "picazón", "urticaria", "ronchas", "picadura", "ansioso", "pánico", "puedo dormir", "abrumado", "agotado")
            "IT" -> listOf("bruciore", "prurito", "orticaria", "pomfi", "puntura", "ansioso", "panico", "riesco a dormire", "sopraffatto", "esaurito")
            "DE" -> listOf("brennen", "jucken", "urtikaria", "quaddeln", "stechen", "ängstlich", "panik", "kann nicht schlafen", "überwältigt", "erschöpft")
            "NL" -> listOf("branden", "jeuk", "urticaria", "galbulten", "steken", "angstig", "paniek", "kan niet slapen", "overweldigd", "uitgeput")
            else -> listOf("burning", "itching", "hives", "welts", "stinging", "anxious", "panicking", "can't sleep", "overwhelmed", "exhausted")
        }
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

    private fun translateAdvice(advice: String, lang: String): String {
        if (lang == "EN") return advice
        
        val defaultAnxiety = "Practice Square-Breathing: Inhale for 4 seconds, hold for 4, exhale for 4, and rest at the bottom for 4."
        val defaultStress = "Drop Your Shoulders: Intentionally unclamp your jaw, lower your shoulders, and relax your tongue."
        val defaultBurnout = "Initiate an Emergency Pause: Stop running. Give yourself non-negotiable permission to rest."
        val defaultPressure = "Enforce an Explicit Pause: When high pressure hits, take a deliberate timeout to lower your heart rate."

        val translations = mapOf(
            defaultAnxiety to mapOf(
                "AR" to "تدرب على التنفس المربع: شهيق لمدة 4 ثوانٍ، كتم النفس لمدة 4 ثوانٍ، زفير لمدة 4 ثوانٍ، والاستراحة في الأسفل لمدة 4 ثوانٍ.",
                "FR" to "Pratiquez la respiration carrée : inspirez pendant 4 secondes, maintenez pendant 4 secondes, expirez pendant 4 secondes et reposez-vous pendant 4 secondes.",
                "ES" to "Practica la respiración cuadrada: inhala durante 4 segundos, mantén durante 4 segundos, exhala durante 4 segundos y descansa durante 4 segundos.",
                "IT" to "Pratica la respirazione quadrata: inspira per 4 secondi, trattieni per 4 secondi, espira per 4 secondi e riposati per 4 secondi.",
                "DE" to "Üben Sie die Quadratatmung: 4 Sekunden lang einatmen, 4 Sekunden lang anhalten, 4 Sekunden lang ausatmen und 4 Sekunden lang pausieren.",
                "NL" to "Oefen vierkant ademhalen: adem 4 seconden in, houd 4 seconden vast, adem 4 seconden uit en rust 4 seconden."
            ),
            defaultStress to mapOf(
                "AR" to "أرخِ كتفيك: أرخِ فكك عمدًا، وأنزل كتفيك، وأرح لسانك.",
                "FR" to "Relâchez vos épaules : desserrez intentionnellement votre mâchoire, abaissez vos épaules et détendez votre langue.",
                "ES" to "Deja caer tus hombros: relaja intencionadamente la mandíbula, baja los hombros y relaja la lengua.",
                "IT" to "Rilassa le spalle: allenta intenzionalmente la mascella, abbassa le spalle e rilassa la lingua.",
                "DE" to "Lassen Sie Ihre Schultern hängen: Lockern Sie bewusst Ihren Kiefer, senken Sie Ihre Schultern und entspannen Sie Ihre Zunge.",
                "NL" to "Laat je schouders hangen: ontspan bewust je kaak, laat je schouders zakken en ontspan je tong."
            ),
            defaultBurnout to mapOf(
                "AR" to "ابدأ توقفًا طارئًا: توقف عن الركض. امنح نفسك إذنًا غير قابل للتفاوض للراحة.",
                "FR" to "Inities une pause d'urgence : arrêtez de courir. Donnez-vous la permission non négociable de vous reposer.",
                "ES" to "Inicia una pausa de emergencia: deja de correr. Date permiso no negociable para descansar.",
                "IT" to "Avvia una pausa di emergenza: smetti di correre. Concediti il permesso non negoziabile di riposare.",
                "DE" to "Leiten Sie eine Notpause ein: Hören Sie auf zu rennen. Geben Sie sich selbst die unverhandelbare Erlaubnis, sich auszuruhen.",
                "NL" to "Start een noodpauze: stop met rennen. Geef jezelf onvoorwaardelijke toestemming om te rusten."
            ),
            defaultPressure to mapOf(
                "AR" to "فرض توقف صريح: عندما يشتد الضغط، خذ مهلة متعمدة لخفض معدل ضربات قلبك.",
                "FR" to "Imposez une pause explicite : en cas de forte pression, faites une pause délibérée pour ralentir votre rythme cardiaque.",
                "ES" to "Impón una pausa explícita: cuando sientas mucha presión, tómate un descanso deliberado para reducir tu ritmo cardíaco.",
                "IT" to "Imponi una pausa esplicita: quando la pressione si fa sentire, prenditi una pausa deliberata per abbassare la frequenza cardiaca.",
                "DE" to "Erzwingen Sie eine explizite Pause: Wenn hoher Druck herrscht, nehmen Sie sich eine bewusste Auszeit, um Ihre Herzfrequenz zu senken.",
                "NL" to "Dwing een expliciete pauze af: als de druk oploopt, neem dan een bewuste time-out om je hartslag te verlagen."
            ),
            "Practice Square-Breathing: Inhale for 4 seconds, hold for 4, exhale for 4, and rest at the bottom for 4. Repeat 3 to 4 times to disrupt an active histamine cascade." to mapOf(
                "AR" to "تدرب على التنفس المربع: شهيق لمدة 4 ثوانٍ، كتم النفس لمدة 4 ثوانٍ، زفير لمدة 4 ثوانٍ، والاستراحة في الأسفل لمدة 4 ثوانٍ. كرر ذلك 3 إلى 4 مرات لتعطيل تدفق الهستامين النشط.",
                "FR" to "Pratiquez la respiration carrée : inspirez pendant 4 secondes, maintenez pendant 4 secondes, expirez pendant 4 secondes et reposez-vous pendant 4 secondes. Répétez 3 à 4 fois pour perturber une cascade d'histamine active.",
                "ES" to "Practica la respiración cuadrada: inhala durante 4 segundos, mantén durante 4 segundos, exhala durante 4 segundos y descansa durante 4 segundos. Repite de 3 a 4 veces para interrumpir una cascada de histamina activa.",
                "IT" to "Pratica la respirazione quadrata: inspira per 4 secondi, trattieni per 4 secondi, espira per 4 secondi e riposati per 4 secondi. Ripeti da 3 a 4 volte per interrompere una cascata di istamina attiva.",
                "DE" to "Üben Sie die Quadratatmung: 4 Sekunden lang einatmen, 4 Sekunden lang anhalten, 4 Sekunden lang ausatmen und 4 Sekunden lang pausieren. Wiederholen Sie dies 3 bis 4 Mal, um eine aktive Histaminkaskade zu unterbrechen.",
                "NL" to "Oefen vierkant ademhalen: adem 4 seconden in, houd 4 seconden vast, adem 4 seconden uit en rust 4 seconden. Herhaal 3 tot 4 keer om een actieve histaminecascade te verstoren."
            ),
            "Acknowledge Present Safety: Remind yourself: \"In this exact moment, I am safe, I am grounded, and I am completely in control of my next step.\"" to mapOf(
                "AR" to "اعتراف بالأمان الحالي: ذكر نفسك: \"في هذه اللحظة بالذات، أنا آمن، أنا ثابت، وأنا مسيطر تمامًا على خطوتي التالية.\"",
                "FR" to "Reconnaissez votre sécurité actuelle : Rappelez-vous : \"En ce moment précis, je suis en sécurité, je suis ancré et je contrôle totalement ma prochaine étape.\"",
                "ES" to "Reconoce tu seguridad actual: Recuerda: \"En este preciso momento, estoy a salvo, conectado a tierra y tengo el control absoluto de mi próximo paso.\"",
                "IT" to "Riconosci la tua sicurezza attuale: Ricorda a te stesso: \"In questo preciso momento sono al sicuro, sono radicato e ho il controllo totale del mio prossimo passo.\"",
                "DE" to "Erkennen Sie die gegenwärtige Sicherheit an: Erinnern Sie sich selbst: „In diesem exakten Moment bin ich in Sicherheit, geerdet und habe die volle Kontrolle über meinen nächsten Schritt.“",
                "NL" to "Erken de huidige veiligheid: Herinner jezelf eraan: \"Op dit exacte moment ben ik veilig, ben ik geaard en heb ik de volledige controle over mijn volgende stap.\""
            ),
            "Shrink the Mountain: You don't have to figure out the whole mountain right now; just focus on the very next step in front of you." to mapOf(
                "AR" to "صغّر الجبل: ليس عليك معرفة الجبل بأكمله الآن؛ فقط ركز على الخطوة التالية أمامك مباشرة.",
                "FR" to "Réduisez la montagne : vous n'avez pas besoin de comprendre toute la montagne pour le moment ; concentrez-vous simplement sur la toute prochaine étape devant vous.",
                "ES" to "Reduce la montaña: no tienes que descifrar toda la montaña ahora mismo; solo concéntrate en el siguiente paso que tienes por delante.",
                "IT" to "Rimpicciolisci la montagna: non devi scalare l'intera montagna in questo momento; concentrati solo sul passo successivo direttamente di fronte a te.",
                "DE" to "Verkleinern Sie den Berg: Sie müssen nicht gleich den ganzen Berg bezwingen; konzentrieren Sie sich einfach auf den allernächsten Schritt vor Ihnen.",
                "NL" to "Maak de berg kleiner: je hoeft nu niet de hele berg te overzien; concentreer je gewoon op de allereerste volgende stap voor je."
            ),
            "Anchor in the Minute: The future will handle itself. Your only job right now is to anchor yourself in this exact minute." to mapOf(
                "AR" to "ثبّت نفسك في الدقيقة الحالية: سيهتم المستقبل بنفسه. وظيفتك الوحيدة الآن هي تثبيت نفسك في هذه الدقيقة بالذات.",
                "FR" to "Ancrez-vous dans la minute : l'avenir s'occupera de lui-même. Votre seul travail en ce moment est de vous ancrer dans cette minute précise.",
                "ES" to "Anclate en el minuto: el futuro se cuidará solo. Tu único trabajo en este momento es anclarte en este minuto exacto.",
                "IT" to "Ancorati al minuto: il futuro si gestirà da solo. Il tuo unico compito in questo momento é ancorarti a questo preciso minuto.",
                "DE" to "Verankern Sie sich in der Minute: Die Zukunft wird sich um sich selbst kümmern. Ihre einzige Aufgabe im Moment ist es, sich in dieser exakten Minute zu verankern.",
                "NL" to "Anker in de minuut: de toekomst regelt zichzelf. Je enige taak nu is om jezelf in deze exacte minuut te verankeren."
            ),
            "Focus on Controllables: Let go of what you cannot control right now, and pour your energy into what you can." to mapOf(
                "AR" to "التركيز على ما يمكنك التحكم به: اترك ما لا يمكنك التحكم فيه الآن، وصب طاقتك فيما يمكنك التحكم فيه.",
                "FR" to "Concentrez-vous sur ce qui est contrôlable : Lâchez ce que vous ne pouvez pas contrôler pour le moment et concentrez votre énergie sur ce que vous pouvez contrôler.",
                "ES" to "Concéntrate en lo que puedes controlar: deja ir lo que no puedes controlar en este momento y vierte tu energía en lo que sí puedes.",
                "IT" to "Concentrati su ciò che puoi controllare: lascia andare ciò che non puoi controllare in questo momento e riversa le tue energie in ciò che puoi.",
                "DE" to "Konzentrieren Sie sich auf das Kontrollierbare: Lassen Sie los, was Sie gerade nicht kontrollieren können, und stecken Sie Ihre Energie in das, was Sie können.",
                "NL" to "Focus op wat je kunt controleren: laat los wat je nu niet kunt controleren en steek je energie in wat je wel kunt."
            ),
            "Engage a 5-4-3-2-1 Sensory Check: Name 5 things you can see, 4 you can touch, 3 you can hear, 2 you can smell, and 1 you can taste to stop a panic loop." to mapOf(
                "AR" to "قم بفحص حسي 5-4-3-2-1: سمِّ 5 أشياء يمكنك رؤيتها، و4 يمكنك لمسها، و3 يمكنك سماعها، و2 يمكنك شمها، و1 يمكنك تذوقها لإيقاف حلقة الذعر.",
                "FR" to "Engagez un contrôle sensoriel 5-4-3-2-1 : Nommez 5 choses que vous pouvez voir, 4 que vous pouvez toucher, 3 que vous pouvez entendre, 2 que vous pouvez sentir et 1 que vous pouvez goûter pour arrêter une boucle de panique.",
                "ES" to "Realiza un control sensorial 5-4-3-2-1: nombra 5 cosas que puedas ver, 4 que puedas tocar, 3 que puedas oír, 2 que puedas oler y 1 que puedas probar para detener un bucle de pánico.",
                "IT" to "Esegui un controllo sensoriale 5-4-3-2-1: nomina 5 cose che puoi vedere, 4 che puoi toccare, 3 che puoi sentire, 2 che puoi odorare e 1 che puoi gustare per fermare un ciclo di panico.",
                "DE" to "Führen Sie einen sensorischen 5-4-3-2-1-Check durch: Nennen Sie 5 Dinge, die Sie sehen können, 4, die Sie berühren können, 3, die Sie hören können, 2, die Sie riechen können, und 1, das Sie schmecken können, um eine Panikschleife zu stoppen.",
                "NL" to "Voer een 5-4-3-2-1 zintuiglijke controle uit: noem 5 dingen die je kunt zien, 4 die je kunt aanraken, 3 die je kunt horen, 2 die je kunt ruiken en 1 die je kunt proeven om een panieklus te stoppen."
            ),
            "Cool Your Body Temperature: Splash cold water on your face or hold an ice pack to stimulate the vagus nerve and slow your heart rate." to mapOf(
                "AR" to "برد درجة حرارة جسمك: رش الماء البارد على وجهك أو ضع كيس ثلج لتحفيز العصب الحائر وإبطاء معدل ضربات قلبك.",
                "FR" to "Refroidissez votre température corporelle : aspergez votre visage d'eau froide ou appliquez une poche de glace pour stimuler le nerf vague et ralentir votre rythme cardiaque.",
                "ES" to "Enfría tu temperatura corporal: salpica agua fría en tu cara o sostén una bolsa de hielo para estimular el nervio vago y ralentizar tu ritmo cardíaco.",
                "IT" to "Raffredda la temperatura corporea: spruzza acqua fredda sul viso o tieni una borsa del ghiaccio per stimolare il nervo vago e rallentare la frequenza cardiaca.",
                "DE" to "Kühlen Sie Ihre Körpertemperatur ab: Spritzen Sie kaltes Wasser auf Ihr Gesicht oder halten Sie einen Eisbeutel, um den Vagusnerv zu stimulieren und Ihre Herzfrequenz zu senken.",
                "NL" to "Koel je lichaamstemperatuur af: sprenkel koud water op je gezicht of houd een ijspak vast om de nervus vagus te stimuleren en je hartslag te verlagen."
            ),
            "Trust Your Resilience: Remind yourself that you have successfully survived 100% of your hardest days so far." to mapOf(
                "AR" to "ثق بمرونتك: ذكر نفسك أنك نجحت في البقاء على قيد الحياة في 100% من أصعب أيامك حتى الآن.",
                "FR" to "Faites confiance à votre résilience : Rappelez-vous que vous avez survécu avec succès à 100 % de vos jours les plus difficiles jusqu'à présent.",
                "ES" to "Confía en tu resiliencia: recuerda que has sobrevivido con el 100 % de tus días más difíciles hasta ahora.",
                "IT" to "Fidati della tua resilienza: ricorda a te stesso che finora sei sopravvissuto con successo al 100% dei tuoi giorni più difficili.",
                "DE" to "Vertrauen Sie auf Ihre Resilienz: Erinnern Sie sich daran, dass Sie bisher 100 % Ihrer schwersten Tage erfolgreich überstanden haben.",
                "NL" to "Vertrouw op je veerkracht: herinner jezelf eraan dat je tot nu toe 100% van je moeilijkste dagen met succes hebt overleefd."
            ),
            "Interrupt the Feedback Loop: Recognize that stress and physical pain feed each other. Disrupting the emotional panic breaks the biological cycle." to mapOf(
                "AR" to "قاطع حلقة التغذية الراجعة: أدرك أن التوتر والألم الجسدي يغذي كل منهما الآخر. يؤدي تعطيل الذعر العاطفي إلى كسر الدورة البيولوجية.",
                "FR" to "Interrompez la boucle de rétroaction : reconnaissez que le stress et la douleur physique se nourrissent mutuellement. Perturber la panique émotionnelle brise le cycle biologique.",
                "ES" to "Interrumpe el bucle de retroalimentación: reconoce que el estrés y el dolor físico se alimentan mutuamente. Interrumpir el pánico emocional rompe el ciclo biológico.",
                "IT" to "Interrompi il ciclo di feedback: riconosci che lo stress e il dolore fisico si alimentano a vicenda. Interrompere il panico emotivo spezza il ciclo biologico.",
                "DE" to "Unterbrechen Sie die Feedbackschleife: Erkennen Sie, dass Stress und körperlicher Schmerz sich gegenseitig nähren. Die Unterbrechung der emotionalen Panik durchbricht den biologischen Kreislauf.",
                "NL" to "Onderbreek de feedbacklus: erken dat stress en fysieke pijn elkaar voeden. Het doorbreken van de emotionele paniek doorbreekt de biologische cyclus."
            ),
            "Commit to the 4-Second Hold: Use the holding phases of square-breathing to physically signal your vagus nerve to stop dumping stress hormones." to mapOf(
                "AR" to "التزم بكتم النفس لمدة 4 ثوانٍ: استخدم مراحل كتم النفس في التنشن المربع لإرسال إشارة جسدية إلى العصب الحائر لوقف إفراز هرمونات التوتر.",
                "FR" to "Engagez-vous à maintenir pendant 4 secondes : utilisez les phases de maintien de la respiration carrée pour signaler physiquement à votre nerf vague d'arrêter de libérer des hormones de stress.",
                "ES" to "Comprométete a mantener la respiración durante 4 segundos: utiliza las fases de retención de la respiración cuadrada para indicar físicamente a tu nervio vago que deje de liberar hormonas del estrés.",
                "IT" to "Impegnati a trattenere il respiro per 4 secondi: usa le fasi di apnea della respirazione quadrata per segnalare fisicamente al tuo nervo vago di smettere di rilasciare ormoni dello stress.",
                "DE" to "Verpflichten Sie sich zum 4-Sekunden-Anhalten: Nutzen Sie die Haltephasen der Quadratatmung, um Ihrem Vagusnerv physisch zu signalisieren, die Ausschüttung von Stresshormonen zu stoppen.",
                "NL" to "Houd je aan de 4 seconden vasthouden: gebruik de vasthoudfasen van de vierkante ademhaling om je nervus vagus fysiek te signaleren dat hij moet stoppen met het afgeven van stresshormonen."
            ),
            "Drop Your Shoulders: Intentionally unclamp your jaw, lower your shoulders away from your ears, and relax your tongue from the roof of your mouth." to mapOf(
                "AR" to "أرخِ كتفيك: أرخِ فكك عمدًا، وأنزل كتفيك بعيدًا عن أذنيك، وأرح لسانك من سقف فمك.",
                "FR" to "Relâchez vos épaules : desserrez intentionnellement votre mâchoire, abaissez vos épaules loin de vos oreilles et détendez votre langue du palais.",
                "ES" to "Deja caer tus hombros: relaja intencionadamente la mandíbula, baja los hombros alejándolos de las orejas y relaja la lengua del paladar.",
                "IT" to "Rilassa le spalle: allenta intenzionalmente la mascella, allontana le spalle dalle orecchie e allontana la lingua dal palato.",
                "DE" to "Lassen Sie Ihre Schultern hängen: Lockern Sie bewusst Ihren Kiefer, senken Sie Ihre Schultern weit weg von Ihren Ohren und lösen Sie Ihre Zunge vom Gaumen.",
                "NL" to "Laat je schouders hangen: ontspan bewust je kaak, laat je schouders zakken, weg van je oren, en haal je tong van je gehemelte."
            ),
            "Initiate an Emergency Pause: Stop running. When you are burned out, your first assignment is to stop trying to force your normal pace." to mapOf(
                "AR" to "ابدأ توقفًا طارئًا: توقف عن الركض. عندما تكون محترقًا نفسيًا، فإن مهمتك الأولى هي التوقف عن محاولة فرض وتيرتك العادية.",
                "FR" to "Inities une pause d'urgence : arrêtez de courir. Lorsque vous êtes épuisé, votre première tâche consiste à cesser d'essayer de forcer votre rythme habituel.",
                "ES" to "Inicia una pausa de emergencia: deja de correr. Cuando estés agotado, tu primera tarea es dejar de intentar forzar tu ritmo normal.",
                "IT" to "Avvia una pausa di emergenza: smetti di correre. Quando sei in burnout, il tuo primo compito è smettere di sforzarti di mantenere il tuo ritmo normale.",
                "DE" to "Leiten Sie eine Notpause ein: Hören Sie auf zu rennen. Wenn Sie ausgebrannt sind, besteht Ihre erste Aufgabe darin, nicht mehr zu versuchen, Ihr normales Tempo zu erzwingen.",
                "NL" to "Start een noodpauze: stop met rennen. Als je opgebrand bent, is je eerste taak om te stoppen met het forceren van je normale tempo."
            ),
            "Accept the Exhaustion: Stop fighting the fatigue. Admitting that your condition is physically punishing and mentally exhausting is the first step." to mapOf(
                "AR" to "تقبل الإرهاق: توقف عن محاربة التعب. الاعتراف بأن حالتك عقاب جسدي ومرهقة عقليًا هو الخطوة الأولى.",
                "FR" to "Acceptez l'épuisement : arrêtez de combattre la fatigue. Admettre que votre état est physiquement éprouvant et mentalement épuisant est la première étape.",
                "ES" to "Acepta el agotamiento: deja de luchar contra la fatiga. Admitir que tu condición es físicamente castigadora y mentalmente agotadora es el primer paso.",
                "IT" to "Accetta l'esaurimento: smetti di combattere la fatica. Ammettere che la tua condizione è fisicamente punitiva e mentalmente estenuante è il primo passo.",
                "DE" to "Akzeptieren Sie die Erschöpfung: Hören Sie auf, gegen die Müdigkeit anzukämpfen. Zuzugeben, dass Ihr Zustand körperlich anstrengend und geistig erschöpfend ist, ist der erste Schritt.",
                "NL" to "Accepteer de uitputting: stop met vechten tegen de vermoeidheid. Toeven dat je toestand fysiek slopend en mentaal uitputtend is, is de eerste stap."
            ),
            "Drop the Guilt: You are not lazy; you are empty. Give yourself absolute, non-negotiable permission to rest without self-reproach." to mapOf(
                "AR" to "تخلص من الذنب: أنت لست كسولاً؛ أنت فارغ فقط. امنح نفسك إذنًا مطلقًا وغير قابل للتفاوض للراحة دون لوم نفسك.",
                "FR" to "Abandonnez la culpabilité : vous n'êtes pas paresseux ; vous êtes vide. Donnez-vous la permission absolue et non négociable de vous reposer sans culpabiliser.",
                "ES" to "Olvídate de la culpa: no eres perezoso; estás vacío. Date permiso absoluto y no negociable para descansar sin reproches.",
                "IT" to "Metti da parte il senso di colpa: non sei pigro; sei solo vuoto. Concediti il permesso assoluto e non negoziabile di riposare senza rimproverarti.",
                "DE" to "Legen Sie die Schuldgefühle ab: Sie sind nicht faul; Sie sind leer. Geben Sie sich selbst die absolute, unverhandelbare Erlaubnis, sich ohne Selbstvorwürfe auszuruhen.",
                "NL" to "Laat het schuldgevoel los: je bent niet lui; je bent leeg. Geef jezelf absolute, niet-onderhandelbare toestemming om te rusten zonder zelfverwijt."
            ),
            "Enforce an Explicit Pause: When high pressure hits, take a deliberate timeout to lower your heart rate before responding." to mapOf(
                "AR" to "فرض توقف صريح: عندما يشتد الضغط، خذ مهلة متعمدة لخفض معدل ضربات قلبك قبل الرد.",
                "FR" to "Imposez une pause explicite : en cas de forte pression, faites une pause délibérée pour ralentir votre rythme cardiaque avant de répondre.",
                "ES" to "Impón una pausa explícita: cuando sientas mucha presión, tómate un descanso deliberado para reducir tu ritmo cardíaco antes de responder.",
                "IT" to "Imponi una pausa esplicita: quando la pressione si fa sentire, prenditi una pausa deliberata per abbassare la frequenza cardiaca prima di rispondere.",
                "DE" to "Erzwingen Sie eine explizite Pause: Wenn hoher Druck herrscht, nehmen Sie sich eine bewusste Auszeit, um Ihre Herzfrequenz zu senken, bevor Sie reagieren.",
                "NL" to "Dwing een expliciete pauze af: als de druk oploopt, neem dan een bewuste time-out om je hartslag te verlagen voordat je reageert."
            ),
            "Execute Your Breathing Tool: Use structured square-breathing to physically disrupt the active stress cascade." to mapOf(
                "AR" to "نفذ أداة التنفس الخاصة بك: استخدم التنفس المربع المنظم لتعطيل تدفق الضغط النشط جسديًا.",
                "FR" to "Exécutez votre outil de respiration : utilisez une respiration carrée structurée pour perturber physiquement la cascade de stress active.",
                "ES" to "Ejecuta tu herramienta de respiración: utiliza la respiración cuadrada estructurada para interrumpir físicamente la cascada de estrés activa.",
                "IT" to "Usa lo strumento di respirazione: utilizza la respirazione quadrata strutturata per interrompere fisicamente la cascata di stress attiva.",
                "DE" to "Nutzen Sie Ihre Atemübung: Verwenden Sie die strukturierte Quadratatmung, um die aktive Stresskaskade physisch zu unterbrechen.",
                "NL" to "Gebruik je ademhalingstool: gebruik gestructureerde vierkante ademhaling om de actieve stresscascade fysiek te verstoren."
            ),
            "Anchor in Real-Time Safety: Ground yourself by stating clearly: \"In this exact moment, I am safe, I am stable, and I am in control of my very next step.\"" to mapOf(
                "AR" to "الترسيخ في الأمان الفعلي: ثبّت نفسك بالقول بوضوح: \"في هذه اللحظة بالذات، أنا آمن، أنا مستقر، وأنا مسيطر على خطوتي التالية مباشرة.\"",
                "FR" to "Ancrez-vous dans la sécurité en temps réel : Ancrez-vous en affirmant clairement : \"En ce moment précis, je suis en sécurité, je suis stable et je contrôle ma toute prochaine étape.\"",
                "ES" to "Anclate en la seguridad en tiempo real: conéctate afirmando claramente: \"En este preciso momento, estoy a salvo, estable y tengo el control de mi próximo paso inmediato\".",
                "IT" to "Ancorati alla sicurezza in tempo reale: radicati affermando chiaramente: \"In questo preciso momento sono al sicuro, sono stabile e ho il controllo del mio prossimo passo.\"",
                "DE" to "Verankern Sie sich in der Echtzeit-Sicherheit: Erden Sie sich, indem Sie klar sagen: „In diesem exakten Moment bin ich in Sicherheit, ich bin stabil und ich kontrolliere meinen allernächsten Schritt.“",
                "NL" to "Anker in real-time veiligheid: aard jezelf door duidelijk te stellen: \"Op dit exacte moment ben ik veilig, stabiel en heb ik de controle over mijn allereerste volgende stap.\""
            )
        )
        val translatedMap = translations[advice]
        return translatedMap?.get(lang) ?: advice
    }

    private fun getUrtiResponse(userText: String): String {
        // Compile the comprehensive 3-Layer context block dynamically
        val contextBlock = contextAssembler.assembleContextBlock(_currentChat.value)
        val text = userText.lowercase()
        val lang = currentChatLanguage.value

        // 1. Safety Bound Intercept check
        val isSafetySwelling = when (lang) {
            "AR" -> text.contains("حلق") || text.contains("توزم") || text.contains("تورم") || 
                    text.contains("لسان") || text.contains("تنفس") || 
                    text.contains("ضيق") || text.contains("اختناق")
            "FR" -> text.contains("gorge") || text.contains("gonflement") || 
                    text.contains("langue") || text.contains("respiration") || 
                    text.contains("oppression") || text.contains("serrement") || text.contains("étouffement")
            "ES" -> text.contains("garganta") || text.contains("hinchazón") || 
                    text.contains("lengua") || text.contains("respiración") || 
                    text.contains("opresión") || text.contains("asfixia") || text.contains("ahogo")
            "IT" -> text.contains("gola") || text.contains("gonfiore") || 
                    text.contains("lingua") || text.contains("respirazione") || 
                    text.contains("oppressione") || text.contains("soffocamento")
            "DE" -> text.contains("hals") || text.contains("kehle") || text.contains("schwellung") || 
                    text.contains("zunge") || text.contains("atmung") || 
                    text.contains("enge") || text.contains("erstickung")
            "NL" -> text.contains("keel") || text.contains("zwelling") || 
                    text.contains("tong") || text.contains("ademhaling") || 
                    text.contains("beklemming") || text.contains("verstikking")
            else -> text.contains("throat") || text.contains("swelling") || 
                    text.contains("tongue") || text.contains("breathing") || 
                    text.contains("tightness") || text.contains("choking")
        }
        if (isSafetySwelling) {
            return when (lang) {
                "AR" -> "أسمع كم هذا مخيف، ولكن بما أنك ذكرت تورمًا أو صعوبة في التنفس، فقد يكون هذا تفاعلًا تحسسيًا شديدًا يهدد الحياة (حساسية مفرطة). يرجى طلب الرعاية الطبية الفورية أو الاتصال بخدمات الطوارئ الآن. سلامتك الجسدية هي الأولوية القصوى المطلقة. أورتي يساندك، ولكن يرجى الاتصال بخدمات الطوارئ على الفور."
                "FR" -> "Je comprends à quel point c'est effrayant, mais comme vous avez mentionné un gonflement ou des difficultés respiratoires, il pourrait s'agir d'une réaction allergique grave et potentiellement mortelle (anaphylaxie). Veuillez consulter immédiatement un médecin professionnel ou appeler les services d'urgence dès maintenant. Votre sécurité physique est la priorité absolue. Urti est là pour vous soutenir, mais veuillez contacter les services d'urgence immédiatement."
                "ES" -> "Entiendo lo aterrador que es esto, pero como mencionaste hinchazón o dificultad para respirar, esto podría ser una reacción alérgica grave y potencialmente mortal (anafilaxia). Por favor, busca atención médica profesional inmediata o llama a los servicios de emergencia ahora mismo. Tu seguridad física es la prioridad absoluta. Urti te acompaña, pero por favor contacta a los servicios de emergencia de inmediato."
                "IT" -> "Capisco quanto sia spaventoso, ma poiché hai menzionato gonfiore o difficoltà respiratorie, potrebbe trattarsi di una reazione allergica grave e potenzialmente letale (anafilassi). Si prega di cercare immediatamente assistenza medica professionale o di chiamare subito i servizi di emergenza. La tua sicurezza fisica è l'assoluta priorità. Urti è qui per te, ma contatta immediatamente i servizi di emergenza."
                "DE" -> "Ich verstehe, wie beängstigend das ist, aber da Sie eine Schwellung oder Atembeschwerden erwähnt haben, könnte dies eine schwere, lebensbedrohliche allergische Reaktion (Anaphylaxie) sein. Bitte suchen Sie sofort professionelle medizinische Hilfe auf oder rufen Sie jetzt den Notruf an. Ihre körperliche Sicherheit hat die allerhöchste Priorität. Urti is für Sie da, aber bitte wenden Sie sich sofort an den Rettungsdienst."
                "NL" -> "Ik begrijp hoe beangstigend dit is, maar aangezien je zwelling of ademhalingsmoeilijkheden noemde, kan dit een ernstige, levensbedreigende allergische reactie (anafylaxie) zijn. Zoek onmiddellijk professionele medische hulp of bel nu de hulpdiensten. Je fysieke veiligheid is de allerhoogste prioriteit. Urti is er voor je, maar neem direct contact op met de hulpdiensten."
                else -> "I hear how frightening this is, but since you mentioned swelling or difficulty breathing, this could be a severe, life-threatening allergic reaction (anaphylaxis). Please seek immediate professional medical care or call emergency services right now. Your physical safety is the absolute first priority. Urti is holding space for you, but please contact emergency services immediately."
            }
        }

        // 2. Compliance Alerts check (Unsafe window < 8 hours)
        val hasMedicationIntervalWarning = contextBlock.contains("CRITICAL MEDICATION ALERT")
        val complianceWarningText = if (hasMedicationIntervalWarning) {
            when (lang) {
                "AR" -> "\n\n*ملاحظة من أورتي: لاحظت أن إدخالات الأدوية الأخيرة تم تسجيلها في فترة أقل من 8 ساعات. يرجى أن تكون لطيفًا مع جسدك والتأكد من اتباع بروتوكولات الجرعات الآمنة.*"
                "FR" -> "\n\n*Note d'Urti : J'ai remarqué que vos dernières prises de médicaments ont été enregistrées à moins de 8 heures d'intervalle. S'il vous plaît, soyez doux avec votre corps et assurez-vous de suivre des protocoles de dosage sûrs.*"
                "ES" -> "\n\n*Nota de Urti: Noté que tus registros recientes de medicamentos se realizaron con menos de 8 horas de diferencia. Por favor, sé amable con tu cuerpo y asegúrate de seguir protocolos de dosificación seguros.*"
                "IT" -> "\n\n*Nota di Urti: Ho notato che le tue recenti registrazioni di farmaci sono state inserite a meno di 8 ore di distanza. Sii gentile con il tuo corpo e assicurati di seguire protocolli di dosaggio sicuri.*"
                "DE" -> "\n\n*Hinweis von Urti: Ich habe bemerkt, dass Ihre letzten Medikamenteneinträge weniger als 8 Stunden auseinander lagen. Bitte gehen Sie schonend mit Ihrem Körper um und stellen Sie sicher, dass Sie sichere Dosierungsprotokolle befolgen.*"
                "NL" -> "\n\n*Opmerking van Urti: Ik merkte dat je recente medicatie-invoer minder dan 8 uur uit elkaar lag. Wees voorzichtig met je lichaam en zorg ervoor dat je veilige doseringsprotocollen volgt.*"
                else -> "\n\n*Note from Urti: I noticed your recent medication entries were logged less than 8 hours apart. Please be gentle with your body and make sure you are following safe dosing protocols.*"
            }
        } else {
            ""
        }

        // 3. Activity Stream Analysis
        val last4EntriesDescription = if (!contextBlock.contains("Fallback Active")) {
            val lastLog = dbHelper.getAllEntries().firstOrNull()
            if (lastLog != null) {
                val actionDesc = when (lastLog.type) {
                    EntryType.FLARE_UP -> when (lang) {
                        "AR" -> "لقد لاحظت أنك قمت بتسجيل نوبة تهيج مؤخرًا."
                        "FR" -> "J'ai remarqué que vous avez enregistré une poussée récemment."
                        "ES" -> "Noté que registraste un brote recientemente."
                        "IT" -> "Ho notato che hai registrato una riacutizzazione di recente."
                        "DE" -> "Ich habe bemerkt, dass Sie kürzlich einen Schub protokolliert haben."
                        "NL" -> "Ik merkte dat je onlangs een opvlamming hebt gelogd."
                        else -> "I noticed you logged a Flare-up recently."
                    }
                    EntryType.ANTIHISTAMINE -> when (lang) {
                        "AR" -> "لقد لاحظت أنك قمت بتسجيل تناول مضاد للهستامين مؤخرًا."
                        "FR" -> "J'ai remarqué que vous avez enregistré la prise d'un antihistaminique récemment."
                        "ES" -> "Noté que registraste haber tomado un antihistamínico recientemente."
                        "IT" -> "Ho notato che hai registrato l'assunzione di un antistaminico di recente."
                        "DE" -> "Ich habe bemerkt, dass Sie kürzlich die Einnahme eines Antihistaminikums protokolliert haben."
                        "NL" -> "Ik merkte dat je onlangs het innemen van een antihistaminicum hebt gelogd."
                        else -> "I noticed you logged taking an Antihistamine recently."
                    }
                    EntryType.CORTISONE -> when (lang) {
                        "AR" -> "أرى أنك قمت بتسجيل جرعة كورتيزون مؤخرًا."
                        "FR" -> "Je vois que vous avez enregistré une dose de cortisone récemment."
                        "ES" -> "Veo que registraste una dosis de cortisona recientemente."
                        "IT" -> "Vedo che hai registrato una dose di cortisone di recente."
                        "DE" -> "Ich sehe, Sie haben kürzlich eine Kortisondosis protokolliert."
                        "NL" -> "Ik zie dat je onlangs een dosis cortison hebt gelogd."
                        else -> "I see you logged a Cortisone dose recently."
                    }
                    EntryType.XOLAIR_150 -> when (lang) {
                        "AR" -> "أرى أنك قمت بتسجيل حقنة زولير 150 ملغ مؤخرًا."
                        "FR" -> "Je vois que vous avez enregistré votre injection de Xolair 150 mg récemment."
                        "ES" -> "Veo que registraste tu inyección de Xolair 150 mg recientemente."
                        "IT" -> "Vedo che hai registrato la tua iniezione di Xolair 150 mg di recente."
                        "DE" -> "Ich sehe, Sie haben kürzlich Ihre Xolair 150 mg-Injektion protokolliert."
                        "NL" -> "Ik zie dat je onlangs je Xolair 150 mg injectie hebt gelogd."
                        else -> "I see you logged your Xolair 150 mg injection recently."
                    }
                    EntryType.XOLAIR_300 -> when (lang) {
                        "AR" -> "أرى أنك قمت بتسجيل حقنة زولير 300 ملغ مؤخرًا."
                        "FR" -> "Je vois que vous avez enregistré votre injection de Xolair 300 mg récemment."
                        "ES" -> "Veo que registraste tu inyección de Xolair 300 mg recientemente."
                        "IT" -> "Vedo che hai registrato la tua iniezione di Xolair 300 mg di recente."
                        "DE" -> "Ich sehe, Sie haben kürzlich Ihre Xolair 300 mg-Injektion protokolliert."
                        "NL" -> "Ik zie dat je onlangs je Xolair 300 mg injectie hebt gelogd."
                        else -> "I see you logged your Xolair 300 mg injection recently."
                    }
                    EntryType.ALTERNATIVE -> when (lang) {
                        "AR" -> "لقد لاحظت أنك قمت بتسجيل دواء بديل (${lastLog.metadata ?: "غير مسمى"}) مؤخرًا."
                        "FR" -> "J'ai remarqué que vous avez enregistré un médicament alternatif (${lastLog.metadata ?: "Unnamed"}) récemment."
                        "ES" -> "Noté que registraste un medicamento alternativo (${lastLog.metadata ?: "Unnamed"}) recientemente."
                        "IT" -> "Ho notato che hai registrato un farmaco alternativo (${lastLog.metadata ?: "Unnamed"}) di recente."
                        "DE" -> "Ich habe bemerkt, dass Sie kürzlich ein alternatives Medikament (${lastLog.metadata ?: "Unnamed"}) protokolliert haben."
                        "NL" -> "Ik merkte dat je onlangs alternatieve medicatie (${lastLog.metadata ?: "Unnamed"}) hebt gelogd."
                        else -> "I noticed you logged alternative medication (${lastLog.metadata ?: "Unnamed"}) recently."
                    }
                    EntryType.CONSUMPTION -> {
                        val parts = lastLog.metadata?.split(":::")
                        val itemName = if (parts != null && parts.size >= 2 && parts[0] == "Consumption") parts[1] else "Consumption item"
                        when (lang) {
                            "AR" -> "لقد لاحظت أنك سجلت استهلاك $itemName مؤخرًا."
                            "FR" -> "J'ai remarqué que vous avez enregistré la consommation de $itemName récemment."
                            "ES" -> "Noté que registraste el consumo de $itemName recientemente."
                            "IT" -> "Ho notato che hai registrato il consumo di $itemName di recente."
                            "DE" -> "Ich habe bemerkt, dass Sie kürzlich den Verzehr von $itemName protokolliert haben."
                            "NL" -> "Ik merkte dat je onlangs de consumptie van $itemName hebt gelogd."
                            else -> "I noticed you logged consumption of $itemName recently."
                        }
                    }
                }
                val followUp = when (lang) {
                    "AR" -> " دعنا نركز على تهدئة الجسم وإرخاء الجهاز العصبي."
                    "FR" -> " Concentrons-nous sur le calme du corps et la relaxation du système nerveux."
                    "ES" -> " Enfoquémonos en calmar el cuerpo y relajar el sistema nervioso."
                    "IT" -> " Concentriamoci sul calmare il corpo e rilassare il sistema nervoso."
                    "DE" -> " Lassen Sie uns darauf konzentrieren, den Körper zu beruhigen und das Nervensystem zu entspannen."
                    "NL" -> " Laten we ons concentreren op het kalmeren van het lichaam en het ontspannen van het zenuwstelsel."
                    else -> " Let's focus on calming down the body and relaxing the nervous system."
                }
                "$actionDesc$followUp"
            } else {
                ""
            }
        } else {
            ""
        }

        // 4. Sentiment parsing and advice selection
        val responseBody = when {
            text.contains("anxiety") || text.contains("anxious") || text.contains("panic") || text.contains("scared") || text.contains("fear") ||
            (lang == "AR" && (text.contains("قلق") || text.contains("خوف") || text.contains("هلع") || text.contains("خائف"))) ||
            (lang == "FR" && (text.contains("anxiété") || text.contains("anxieux") || text.contains("panique") || text.contains("peur") || text.contains("effrayé"))) ||
            (lang == "ES" && (text.contains("ansiedad") || text.contains("ansioso") || text.contains("pánico") || text.contains("asustado") || text.contains("miedo"))) ||
            (lang == "IT" && (text.contains("ansia") || text.contains("ansioso") || text.contains("panico") || text.contains("spaventato") || text.contains("paura"))) ||
            (lang == "DE" && (text.contains("angst") || text.contains("ängstlich") || text.contains("panik") || text.contains("erschrocken") || text.contains("furcht"))) ||
            (lang == "NL" && (text.contains("angst") || text.contains("angstig") || text.contains("paniek") || text.contains("bang") || text.contains("vrees"))) -> {
                val rawAdvice = if (_anxietyAdvice.isNotEmpty()) _anxietyAdvice.random() else "Practice Square-Breathing: Inhale for 4 seconds, hold for 4, exhale for 4, and rest at the bottom for 4."
                val advice = translateAdvice(rawAdvice, lang)
                when (lang) {
                    "AR" -> "أسمعك. يعمل القلق كفتيل كيميائي يأمر الخلايا المناعية بإفراز الهستامين، مما يؤدي إلى ظهور بثور وحكة مؤلمة. خذ لحظة لترسيخ نفسك بهذه النصيحة:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "FR" -> "Je vous entends. L'anxiété agit comme un fusible chimique qui ordonne aux cellules immunitaires de libérer de l'histamine, déclenquant des plaques d'urticaire douloureuses et des démangeaisons. Prenez un moment pour vous ancrer grâce à ce conseil :\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "ES" -> "Te escucho. La ansiedad actúa como un fusible químico que ordena a las células inmunitarias liberar histamina, lo que provoca ronchas dolorosas y picazón. Tómate un momento para anclarte con este consejo:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "IT" -> "Ti ascolto. L'ansia agisce come una miccia chimica che ordina alle cellule immunitarie di rilasciare istamina, scatenando orticaria dolorosa e prurito. Prenditi un momento per ancorarti con questo consiglio:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "DE" -> "Ich höre Sie. Angst wirkt wie eine chemische Zündschnur, die Immunzellen befiehlt, Histamin freizusetzen, was zu schmerzhaften Quaddeln und Juckreiz führt. Nehmen Sie sich einen Moment Zeit, um sich mit diesem Rat zu erden:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "NL" -> "Ik hoor je. Angst werkt als een chemische lont die immuuncellen opdracht geeft om histamine af te geven, wat leidt tot pijnlijke galbulten en jeuk. Neem een moment om jezelf te aarden met dit advies:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    else -> "I hear you. Anxiety acts like a chemical fuse that commands immune cells to dump histamine, triggering painful hives and itching. Take a moment to anchor yourself with this advice:\n\n\"$advice\"\n\n$last4EntriesDescription"
                }
            }
            text.contains("stress") || text.contains("stressed") || text.contains("exhaust") || text.contains("heavy") || text.contains("chaos") ||
            (lang == "AR" && (text.contains("ضغط") || text.contains("مجهد") || text.contains("تعب") || text.contains("ثقيل") || text.contains("فوضى"))) ||
            (lang == "FR" && (text.contains("stress") || text.contains("stressé") || text.contains("épuisé") || text.contains("lourd") || text.contains("chaos"))) ||
            (lang == "ES" && (text.contains("estrés") || text.contains("estresado") || text.contains("agotado") || text.contains("pesado") || text.contains("caos"))) ||
            (lang == "IT" && (text.contains("stress") || text.contains("stressato") || text.contains("esaurito") || text.contains("pesante") || text.contains("caos"))) ||
            (lang == "DE" && (text.contains("stress") || text.contains("gestresst") || text.contains("erschöpft") || text.contains("schwer") || text.contains("chaos"))) ||
            (lang == "NL" && (text.contains("stress") || text.contains("gestrest") || text.contains("uitgeput") || text.contains("zwaar") || text.contains("chaos"))) -> {
                val rawAdvice = if (_stressAdvice.isNotEmpty()) _stressAdvice.random() else "Drop Your Shoulders: Intentionally unclamp your jaw, lower your shoulders, and relax your tongue."
                val advice = translateAdvice(rawAdvice, lang)
                when (lang) {
                    "AR" -> "أشعر بمدى الضغط الذي تتحمله الآن. يأمر الضغط العاطفي الخلايا الصارية حرفيًا بالتخلص من حبيباتها وإفراز الهستامين. دعنا نتدرب على تهدئة جهازك العصبي:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "FR" -> "Je comprends le stress que vous portez en ce moment. La détresse émotionnelle ordonne littéralement aux mastocytes de se dégranuler et de libérer des histamines. Entraînons-nous à calmer votre système nerveux :\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "ES" -> "Entiendo cuánto estrés estás cargando en este momento. El sufrimiento emocional literalmente ordena a los mastocitos desgranularse y liberar histaminas. Practiquemos regular a la baja tu sistema nervioso:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "IT" -> "Capisco quanto stress stai portando in questo momento. Il disagio emotivo ordina letteralmente ai mastociti di degranulare e rilasciare istamine. Pratichiamo la regolazione del sistema nervoso:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "DE" -> "Ich höre, wie viel Stress Sie gerade in sich tragen. Emotionaler Stress befiehlt den Mastzellen buchstäblich, sich zu degranulieren und Histamine freizusetzen. Lassen Sie uns üben, Ihr Nervensystem herunterzuregulieren:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "NL" -> "Ik begrijp hoeveel stress je momenteel met je meedraagt. Emotionele stress geeft mestcellen letterlijk opdracht om te degranuleren en histamines vrij te geven. Laten we oefenen met het kalmeren van je zenuwstelsel:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    else -> "I hear how much stress you are carrying right now. Emotional distress literally commands mast cells to degranulate and release histamines. Let's practice down-regulating your nervous system:\n\n\"$advice\"\n\n$last4EntriesDescription"
                }
            }
            text.contains("burnout") || text.contains("burned out") || text.contains("empty") || text.contains("depleted") || text.contains("tired") ||
            (lang == "AR" && (text.contains("احتراق") || text.contains("منهك") || text.contains("فارغ") || text.contains("مستنزف") || text.contains("تعبان"))) ||
            (lang == "FR" && (text.contains("épuisement") || text.contains("vide") || text.contains("épuisé") || text.contains("fatigué"))) ||
            (lang == "ES" && (text.contains("agotamiento") || text.contains("vacío") || text.contains("agotado") || text.contains("cansado"))) ||
            (lang == "IT" && (text.contains("esaurimento") || text.contains("vuoto") || text.contains("esaurito") || text.contains("stanco"))) ||
            (lang == "DE" && (text.contains("burnout") || text.contains("ausgebrannt") || text.contains("leer") || text.contains("erschöpft") || text.contains("müde"))) ||
            (lang == "NL" && (text.contains("burn-out") || text.contains("opgebrand") || text.contains("leeg") || text.contains("uitgeput") || text.contains("moe"))) -> {
                val rawAdvice = if (_burnoutAdvice.isNotEmpty()) _burnoutAdvice.random() else "Initiate an Emergency Pause: Stop running. Give yourself non-negotiable permission to rest."
                val advice = translateAdvice(rawAdvice, lang)
                when (lang) {
                    "AR" -> "تبدو فارغًا تمامًا. تتطلب الشرى المزمنة والتعافي من الاحتراق النفسي إذنًا غير قابل للتفاوض للتوقف المؤقت. توصية أورتي:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "FR" -> "Vous semblez complètement épuisé. L'urticaire chronique et la récupération après un épuisement professionnel exigent une permission non négociable de faire une pause. La recommandation d'Urti :\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "ES" -> "Suenas completamente agotado. La urticaria crónica y la recuperación del agotamiento requieren un permiso no negociable para hacer una pausa. Recomendación de Urti:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "IT" -> "Sembri completamente esausto. L'orticaria cronica e il recupero dal burnout richiedono il permesso non negoziabile di fare una pausa. La raccomandazione di Urti:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "DE" -> "Sie klingen völlig erschöpft. Chronische Urtikaria und die Genesung von Burnout erfordern die unverhandelbare Erlaubnis, eine Pause einzulegen. Urtis Empfehlung:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "NL" -> "Je klinkt volkomen leeg. Chronische urticaria en herstel van een burn-out vereisen een niet-onderhandelbare toestemming om te pauzeren. Urti's aanbeveling:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    else -> "You sound completely empty. Chronic hives and recovery from burnout require non-negotiable permission to pause. Urti's recommendation:\n\n\"$advice\"\n\n$last4EntriesDescription"
                }
            }
            text.contains("pressure") || text.contains("pressured") || text.contains("work") || text.contains("deadline") || text.contains("busy") ||
            (lang == "AR" && (text.contains("ضغط") || text.contains("عمل") || text.contains("موعد") || text.contains("مشغول"))) ||
            (lang == "FR" && (text.contains("pression") || text.contains("travail") || text.contains("délai") || text.contains("occupé"))) ||
            (lang == "ES" && (text.contains("presión") || text.contains("trabajo") || text.contains("plazo") || text.contains("ocupado"))) ||
            (lang == "IT" && (text.contains("pressione") || text.contains("lavoro") || text.contains("scadenza") || text.contains("occupato"))) ||
            (lang == "DE" && (text.contains("druck") || text.contains("arbeit") || text.contains("frist") || text.contains("beschäftigt"))) ||
            (lang == "NL" && (text.contains("druk") || text.contains("werk") || text.contains("deadline") || text.contains("bezig"))) -> {
                val rawAdvice = if (_pressureAdvice.isNotEmpty()) _pressureAdvice.random() else "Enforce an Explicit Pause: When high pressure hits, take a deliberate timeout to lower your heart rate."
                val advice = translateAdvice(rawAdvice, lang)
                when (lang) {
                    "AR" -> "الضغط الشديد الذي تتعرض له يغلق صدرك وأعصابك. تراجع عن حلقة الضغط للحظة:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "FR" -> "La pression intense que vous subissez bloque votre poitrine et vos nerfs. Sortez de la boucle de pression un instant :\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "ES" -> "La intensa presión bajo la que te encuentras bloquea tu pecho y tus nervios. Sal del bucle de presión por un momento:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "IT" -> "La pressione intensa a cui sei sottoposto blocca il petto e i nervi. Fai un passo indietro dal ciclo di pressione per un momento:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "DE" -> "Der intensive Druck, unter dem Sie stehen, schnürt Ihnen die Brust und die Nerven ein. Treten Sie für einen Moment aus der Druckschleife heraus:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    "NL" -> "De intense druk waaronder je staat, blokkeert je borst en zenuwen. Stap even uit de druklus:\n\n\"$advice\"\n\n$last4EntriesDescription"
                    else -> "The intense pressure you're under locks up your chest and nerves. Step back from the pressure loop for a moment:\n\n\"$advice\"\n\n$last4EntriesDescription"
                }
            }
            text.contains("thank") || text.contains("thanks") || text.contains("helpful") || text.contains("good") || text.contains("happy") || text.contains("better") ||
            (lang == "AR" && (text.contains("شكرا") || text.contains("شكرًا") || text.contains("مفيد") || text.contains("جيد") || text.contains("سعيد") || text.contains("أفضل"))) ||
            (lang == "FR" && (text.contains("merci") || text.contains("utile") || text.contains("bon") || text.contains("bien") || text.contains("heureux") || text.contains("mieux"))) ||
            (lang == "ES" && (text.contains("gracias") || text.contains("útil") || text.contains("bueno") || text.contains("bien") || text.contains("feliz") || text.contains("mejor"))) ||
            (lang == "IT" && (text.contains("grazie") || text.contains("utile") || text.contains("buono") || text.contains("bene") || text.contains("felice") || text.contains("meglio"))) ||
            (lang == "DE" && (text.contains("danke") || text.contains("hilfreich") || text.contains("gut") || text.contains("glücklich") || text.contains("besser"))) ||
            (lang == "NL" && (text.contains("bedankt") || text.contains("dank") || text.contains("nuttig") || text.contains("goed") || text.contains("blij") || text.contains("beter"))) -> {
                when (lang) {
                    "AR" -> listOf(
                        "أنا سعيد جدًا لسماع ذلك! الحفاظ على توازن نظامنا العصبي هو وسيلة جميلة لحماية بشرتك وجسمك. كيف يمكنني دعم توازنك اليوم أيضًا؟",
                        "هذا يجلب السلام المطلق لقلبي. دعم عافيتك هو مهمتي القصوى. احمل هذه المساحة الهادئة والراسخة معك.",
                        "رائع. نحن نتخذ خطوات جميلة معًا نحو السلام والراحة والقوة."
                    ).random()
                    "FR" -> listOf(
                        "Je suis tellement ravi d'entendre cela ! Garder notre système nerveux équilibré est un excellent moyen de protéger votre peau et votre corps. Comment puis-je vous aider d'autre à trouver votre équilibre aujourd'hui ?",
                        "Cela apporte une paix absolue à mon cœur. Soutenir votre bien-être est mon ultime mission. Transportez cet espace calme et ancré avec vous.",
                        "Merveilleux. Nous faisons de beaux pas ensemble vers la paix, le repos et la force."
                    ).random()
                    "ES" -> listOf(
                        "¡Me alegra mucho escuchar eso! Mantener nuestro sistema nervioso equilibrado es una forma hermosa de proteger tu piel y tu cuerpo. ¿De qué otra manera puedo apoyar tu equilibrio hoy?",
                        "Eso trae paz absoluta a mi corazón. Apoyar tu bienestar es mi misión fundamental. Lleva este espacio tranquilo y conectado contigo.",
                        "Maravilloso. Estamos dando pasos hermosos juntos hacia la paz, el descanso y la fuerza."
                    ).random()
                    "IT" -> listOf(
                        "Sono così felice di sentirlo! Mantenere il nostro sistema nervoso equilibrato è un modo bellissimo per proteggere la pelle e il corpo. In quale altro modo posso supportare il tuo equilibrio oggi?",
                        "Questo porta una pace assoluta al mio cuore. Sostenere il tuo benessere è la mia missione finale. Porta con te questo spazio calmo e radicato.",
                        "Meraviglioso. Stiamo facendo passi bellissimi insieme verso la pace, il riposo e la forza."
                    ).random()
                    "DE" -> listOf(
                        "Ich freue mich sehr, das zu hören! Unser Nervensystem im Gleichgewicht zu halten, ist eine wunderbare Möglichkeit, Ihre Haut und Ihren Körper zu schützen. Wie kann ich Sie heute noch bei Ihrem Gleichgewicht unterstützen?",
                        "Das bringt absoluten Frieden in mein Herz. Ihr Wohlbefinden zu unterstützen, ist meine oberste Mission. Tragen Sie diesen ruhigen, geerdeten Ort in sich.",
                        "Wunderbar. Wir gehen gemeinsam schöne Schritte in Richtung Frieden, Ruhe und Kraft."
                    ).random()
                    "NL" -> listOf(
                        "Ik ben zo blij dat te horen! Het in balans houden van ons zenuwstelsel is een prachtige manier om je huid en lichaam te beschermen. Hoe kan ik je vandaag nog meer ondersteunen bij je balans?",
                        "Dat brengt absolute rust in mijn hart. Jouw welzijn ondersteunen is mijn ultieme missie. Neem deze rustige, geaarde ruimte met je mee.",
                        "Prachtig. We zetten samen mooie stappen richting vrede, rust en kracht."
                    ).random()
                    else -> listOf(
                        "I'm so glad to hear that! Keeping our nervous system balanced is a beautiful way to protect your skin and body. How else can I support your balance today?",
                        "That brings absolute peace to my heart. Supporting your well-being is my ultimate mission. Carry this quiet, grounded space with you.",
                        "Wonderful. We are taking beautiful steps together toward peace, rest, and strength."
                    ).random()
                }
            }
            else -> {
                val allAdvice = _anxietyAdvice + _stressAdvice + _burnoutAdvice + _pressureAdvice
                val rawAdvice = if (allAdvice.isNotEmpty()) allAdvice.random() else "Practice Square-Breathing: Inhale for 4 seconds, hold for 4, exhale for 4, and rest at the bottom for 4."
                val advice = translateAdvice(rawAdvice, lang)
                when (lang) {
                    "AR" -> listOf(
                        "شكرًا لمشاركتك ذلك مع أورتي. مشاعرك صالحة تمامًا. دعنا نأخذ قسطًا من الراحة معًا. كيف يشعر جسمك في هذه اللحظة بالذات؟\n\n\"$advice\"",
                        "أنا أستمع إليك. في بعض الأحيان يساعد التنفيس عن مشاعرك في تخفيف التوتر المخزن في كتفينا وفكنا. إرشاد أورتي لك:\n\n\"$advice\"\n\n$last4EntriesDescription",
                        "يبدو أن هذا كثير لتحمله اليوم. تذكر أن تكون لطيفًا مع نفسك. يُسمح لك بأن تكون عملاً قيد التنفيذ وقطعة فنية رائعة في نفس الوقت."
                    ).random()
                    "FR" -> listOf(
                        "Merci de partager cela avec Urti. Vos sentiments sont tout à fait valables. Faisons une pause ensemble. Comment se sent votre corps en ce moment précis ?\n\n\"$advice\"",
                        "Je vous écoute. Parfois, le simple fait d'évacuer aide à relâcher la tension accumulée dans nos épaules et notre mâchoire. Le conseil d'Urti pour vous :\n\n\"$advice\"\n\n$last4EntriesDescription",
                        "Cela semble faire beaucoup à porter aujourd'hui. N'oubliez pas d'être doux avec vous-même. Vous avez le droit d'être à la fois une œuvre en cours et un chef-d'œuvre."
                    ).random()
                    "ES" -> listOf(
                        "Gracias por compartir eso con Urti. Tus sentimientos son completamente válidos. Hagamos una pausa juntos. ¿Cómo se siente tu cuerpo en este preciso momento?\n\n\"$advice\"",
                        "Te escucho. A veces, simplemente desahogarse ayuda a liberar la tensión acumulada en nuestros hombros y mandíbula. Guía de Urti para ti:\n\n\"$advice\"\n\n$last4EntriesDescription",
                        "Eso suena como mucho que soportar hoy. Recuerda ser amable contigo mismo. Se te permite ser una obra en progreso y una obra maestra al mismo tiempo."
                    ).random()
                    "IT" -> listOf(
                        "Grazie per aver condiviso questo con Urti. I tuoi sentimenti sono del tutto validi. Facciamo una pausa insieme. Come si sente il tuo corpo in questo preciso momento?\n\n\"$advice\"",
                        "Ti ascolto. A volte, anche solo sfogarsi aiuta a rilasciare la tensione accumulata nelle spalle e nella mascella. Guida di Urti per te:\n\n\"$advice\"\n\n$last4EntriesDescription",
                        "Sembra che ci sia molto da sopportare oggi. Ricorda di essere gentile con te stesso. Ti è permesso essere un lavoro in corso e un capolavoro allo stesso tempo."
                    ).random()
                    "DE" -> listOf(
                        "Vielen Dank, dass Sie das mit Urti teilen. Ihre Gefühle sind absolut berechtigt. Lassen Sie uns gemeinsam eine Pause einlegen. Wie fühlt sich Ihr Körper in diesem Moment an?\n\n\"$advice\"",
                        "Ich höre zu. Manchmal hilft es einfach, sich Luft zu machen, um die in den Schultern und im Kiefer gespeicherte Spannung abzubauen. Urtis Rat für Sie:\n\n\"$advice\"\n\n$last4EntriesDescription",
                        "Das klingt nach viel, was Sie heute tragen müssen. Denken Sie daran, sanft zu sich selbst zu sein. Sie dürfen gleichzeitig eine Baustelle und ein Meisterwerk sein."
                    ).random()
                    "NL" -> listOf(
                        "Bedankt dat je dit met Urti deelt. Je gevoelens zijn volkomen geldig. Laten we samen even pauzeren. Hoe voelt je lichaam op dit moment?\n\n\"$advice\"",
                        "Ik luister naar je. Soms helpt het uiten van je gevoelens om de spanning in onze schouders en kaak los te laten. Urti's advies voor jou:\n\n\"$advice\"\n\n$last4EntriesDescription",
                        "Dat klinkt als veel om vandaag te dragen. Denk eraan om lief te zijn voor jezelf. Je mag tegelijkertijd een werk in uitvoering en een meesterwerk zijn."
                    ).random()
                    else -> listOf(
                        "Thank you for sharing that with Urti. Your feelings are fully valid. Let's take a pause together. How is your body feeling in this exact moment?\n\n\"$advice\"",
                        "I am listening. Sometimes just venting it out helps release the tension stored in our shoulders and jaw. Urti's guidance for you:\n\n\"$advice\"\n\n$last4EntriesDescription",
                        "That sounds like a lot to hold today. Remember to be gentle with yourself. You are allowed to be a work in progress and a masterpiece at the same time."
                    ).random()
                }
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
            obj.put("language", it.language)
            
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
                        messages = msgList,
                        language = obj.optString("language", "EN")
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

    private fun loadProfileKnownAllergies() {
        val raw = sharedPrefs.getString("profile_known_allergies", "") ?: ""
        if (raw.isEmpty()) {
            _profileKnownAllergies.value = emptyList()
        } else {
            _profileKnownAllergies.value = raw.split(";;;").filter { it.isNotEmpty() }
        }
    }

    fun addProfileKnownAllergy(name: String) {
        val current = _profileKnownAllergies.value.toMutableList()
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && !current.contains(trimmed)) {
            current.add(trimmed)
            _profileKnownAllergies.value = current
            sharedPrefs.edit().putString("profile_known_allergies", current.joinToString(";;;")).apply()
        }
    }

    fun deleteProfileKnownAllergy(name: String) {
        if (_profileKnownAllergies.value.contains(name)) {
            val item = AppDeletedItem.Allergy(name)
            lastDeletedItem.value = item
            viewModelScope.launch {
                delay(6000)
                if (lastDeletedItem.value == item) {
                    lastDeletedItem.value = null
                }
            }
        }
        val current = _profileKnownAllergies.value.toMutableList()
        current.remove(name)
        _profileKnownAllergies.value = current
        sharedPrefs.edit().putString("profile_known_allergies", current.joinToString(";;;")).apply()
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

    fun loadConsumablesState() {
        _recentlyUsedConsumables.value = dbHelper.getRecentlyUsedConsumables(limit = 15)
        _favoriteConsumableIds.value = dbHelper.getFavoriteConsumableIds()
        _favoriteConsumableNames.value = dbHelper.getFavoriteConsumableNames()
    }

    fun searchConsumables(query: String): List<ConsumableItem> {
        return dbHelper.searchConsumables(query, _favoriteConsumableIds.value, _recentlyUsedConsumables.value)
    }

    fun getConsumablesByCategory(category: String): List<ConsumableItem> {
        return dbHelper.getAllConsumables().filter { it.category.equals(category, ignoreCase = true) }
    }

    fun toggleConsumableFavorite(id: String, isFavorite: Boolean) {
        dbHelper.toggleConsumableFavorite(id, isFavorite)
        loadConsumablesState()
    }

    fun isConsumableFavorite(id: String): Boolean {
        return dbHelper.isConsumableFavorite(id)
    }

    fun addCustomConsumable(name: String, category: String, defaultUnit: String): ConsumableItem {
        val item = dbHelper.insertCustomConsumable(name, category, defaultUnit)
        loadConsumablesState()
        return item
    }

    fun getLastUsedQuantityAndUnit(itemName: String): Pair<String, String>? {
        return dbHelper.getLastUsedQuantityAndUnit(itemName)
    }
}


package com.example.urticare20.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.widget.Toast
import com.example.urticare20.model.MedicationReminder
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import com.example.urticare20.model.MenstruationCycle
import com.example.urticare20.model.ProfileCortisone
import com.example.urticare20.model.ProfileOtherMedication
import com.example.urticare20.model.CompletedMedicationCourse
import com.example.urticare20.model.MedicationDirectory
import com.example.urticare20.model.ChronicIllnessesDirectory
import com.example.urticare20.ui.theme.*
import androidx.activity.compose.BackHandler
import com.example.urticare20.viewmodel.TrackerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: TrackerViewModel, onBack: () -> Unit = {}) {
    BackHandler(enabled = true) {
        onBack()
    }

    val context = LocalContext.current

    // Observe persistent profile states from VM
    val profileNameState by viewModel.profileName.collectAsState()
    val profileBirthDateState by viewModel.profileBirthDate.collectAsState()
    val profileAgeState by viewModel.profileAge.collectAsState()
    val profileSexState by viewModel.profileSex.collectAsState()
    val profilePregnantState by viewModel.profilePregnant.collectAsState()
    val profilePregnancyMonthState by viewModel.profilePregnancyMonth.collectAsState()
    val cycles by viewModel.menstruationCycles.collectAsState()
    val antihistamines by viewModel.profileAntihistamines.collectAsState()
    val profileCortisones by viewModel.profileCortisones.collectAsState()
    val profileOtherMedications by viewModel.profileOtherMedications.collectAsState()
    val completedCourses by viewModel.completedMedicationCourses.collectAsState()
    val profileOnXolairState by viewModel.profileOnXolair.collectAsState()

    // Local inputs for editing prior to saving
    var nameInput by remember(profileNameState) { mutableStateOf(profileNameState) }
    var birthDateInput by remember(profileBirthDateState) { mutableStateOf(profileBirthDateState) }
    var sexInput by remember(profileSexState) { mutableStateOf(profileSexState) }
    var pregnantInput by remember(profilePregnantState) { mutableStateOf(profilePregnantState) }
    var monthInput by remember(profilePregnancyMonthState) { mutableStateOf(profilePregnancyMonthState) }
    var onXolairInput by remember(profileOnXolairState) { mutableStateOf(profileOnXolairState) }

    // Dynamic calculated age
    val computedAge = remember(birthDateInput) {
        if (birthDateInput.isNotEmpty()) {
            try {
                val birthDate = LocalDate.parse(birthDateInput)
                val years = ChronoUnit.YEARS.between(birthDate, LocalDate.now())
                "$years"
            } catch (e: Exception) {
                ""
            }
        } else {
            profileAgeState
        }
    }

    // collapsible forms state
    var showAddAntihistamineForm by remember { mutableStateOf(false) }
    var showAddCortisoneForm by remember { mutableStateOf(false) }
    var showAddOtherMedForm by remember { mutableStateOf(false) }
    var showAddIllnessForm by remember { mutableStateOf(false) }

    // Active Cortisone adding state
    var newCortisoneName by remember { mutableStateOf("") }
    var newCortisoneMgs by remember { mutableStateOf("") }
    var editingCortisoneId by remember { mutableStateOf<String?>(null) }

    // Active Other Medication adding state
    var newOtherMedName by remember { mutableStateOf("") }
    var newOtherMedMgs by remember { mutableStateOf("") }
    var newOtherMedCondition by remember { mutableStateOf("") }
    var newOtherMedFrequency by remember { mutableStateOf("") }
    var newOtherMedType by remember { mutableStateOf("") }
    var newOtherMedStartDate by remember { mutableStateOf("") }
    var editingOtherMedId by remember { mutableStateOf<String?>(null) }

    // Urticaria Diagnosis checklist state
    var selectedDiagnoses by remember(viewModel.profileDiagnoses) {
        mutableStateOf(viewModel.profileDiagnoses.value.toSet())
    }

    // Active Antihistamine adding state
    var newMedName by remember { mutableStateOf("") }
    var newMedMgs by remember { mutableStateOf("") }
    var newMedGen by remember { mutableStateOf("") } // "1st", "2nd", or ""
    var editingAntihistamineId by remember { mutableStateOf<String?>(null) }
    var showAutocompleteSuggestions by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Dialog control states
    var showLogDialog by remember { mutableStateOf(false) }
    var cycleToEdit by remember { mutableStateOf<MenstruationCycle?>(null) }
    var showCyclesInMainScreen by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var selectedInfoLanguage by remember { mutableStateOf("English") }
    var showReminderDialogForType by remember { mutableStateOf<String?>(null) }

    // Course completion and sub-tabs states
    var selectedOtherMedTab by remember { mutableIntStateOf(0) }
    var showCourseCompletionDialog by remember { mutableStateOf(false) }
    var selectedMedForCourseCompletion by remember { mutableStateOf<ProfileOtherMedication?>(null) }
    var courseStartDate by remember { mutableStateOf(LocalDate.now().minusDays(7)) }
    var courseEndDate by remember { mutableStateOf(LocalDate.now()) }
    var showPregnancyOverDialog by remember { mutableStateOf(false) }

    LaunchedEffect(nameInput, birthDateInput, computedAge, sexInput, pregnantInput, monthInput, selectedDiagnoses, onXolairInput) {
        viewModel.saveProfile(
            name = nameInput.trim(),
            birthDate = birthDateInput,
            age = computedAge,
            sex = sexInput,
            pregnant = if (sexInput == "F") pregnantInput else "",
            month = if (sexInput == "F" && pregnantInput == "Yes") monthInput.trim() else "",
            diagnoses = selectedDiagnoses,
            cortisoneName = "",
            cortisoneMg = "",
            onXolair = onXolairInput
        )
    }

    // Monthly navigation state for cycles
    var localMonthOffset by remember { mutableIntStateOf(0) }
    val navigatedMonth = remember(localMonthOffset) {
        LocalDate.now().plusMonths(localMonthOffset.toLong())
    }
    val monthYearStr = remember(navigatedMonth) {
        navigatedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    // Filtered menstruation cycles for the active monthly viewer
    val filteredCycles = remember(cycles, navigatedMonth) {
        cycles.filter { c ->
            try {
                val start = LocalDate.parse(c.startDate)
                start.year == navigatedMonth.year && start.monthValue == navigatedMonth.monthValue
            } catch (e: Exception) {
                false
            }
        }
    }

    val diagnosisOptions = listOf(
        "Acute Urticaria",
        "Chronic Spontaneous Urticaria (CSU)",
        "Chronic Inducible Urticaria",
        "Angioedema",
        "Adrenergic Urticaria",
        "Vasculitic Urticaria"
    )

    val infoTexts = mapOf(
        "English" to """
            Antihistamines are among the most commonly used medications for allergies and urticaria (hives). While all antihistamines work by blocking the effects of histamine—a chemical released during allergic reactions—not all antihistamines are the same. They are generally divided into first-generation and second-generation antihistamines, each with distinct characteristics.

            First-Generation Antihistamines

            First-generation antihistamines were developed earlier and readily cross the blood-brain barrier. As a result, they often cause drowsiness, sedation, impaired concentration, and other anticholinergic side effects such as dry mouth and blurred vision.

            Common examples include diphenhydramine, hydroxyzine, chlorpheniramine, and promethazine.

            While these medications can be effective in relieving itching and allergic symptoms, their sedating effects can interfere with daily activities, work, and driving.

            Second-Generation Antihistamines

            Second-generation antihistamines were designed to be more selective and less likely to enter the brain. Consequently, they provide effective allergy and urticaria symptom relief with significantly less sedation.

            Common examples include cetirizine, levocetirizine, loratadine, desloratadine, fexofenadine, bilastine, and rupatadine.

            Because of their improved safety profile and lower risk of drowsiness, second-generation antihistamines are generally preferred for long-term management of chronic urticaria and allergic conditions.

            Key Differences at a Glance
            - Drowsiness: Common with first-generation; minimal with second-generation.
            - Duration of Action: Often shorter with first-generation; generally longer with second-generation.
            - Impact on Daily Activities: Greater impairment with first-generation; less impairment with second-generation.
            - Guideline Preference: Second-generation antihistamines are recommended as first-line treatment for most patients with chronic urticaria.

            Conclusion

            Although both generations effectively block histamine, second-generation antihistamines offer similar symptom control with fewer side effects and better tolerability. For this reason, modern clinical guidelines recommend second-generation antihistamines as the preferred choice for most patients with urticaria and allergic diseases.
        """.trimIndent(),

        "العربية" to """
            تُعد مضادات الهيستامين من أكثر الأدوية استخدامًا لعلاج الحساسية والشرى (الأرتيكاريا). وعلى الرغم من أن جميع مضادات الهيستامين تعمل عن طريق منع تأثير الهيستامين، وهو مادة كيميائية يفرزها الجسم أثناء التفاعلات التحسسية، إلا أنها ليست جميعًا متشابهة. وتنقسم بشكل عام إلى مضادات هيستامين من الجيل الأول ومضادات هيستامين من الجيل الثاني، ولكل منهما خصائصه المميزة.

            مضادات الهيستامين من الجيل الأول

            تم تطوير مضادات الهيستامين من الجيل الأول في وقت مبكر، وتمتاز بقدرتها على عبور الحاجز الدموي الدماغي، مما يجعلها تؤثر على الجهاز العصبي المركزي. لذلك، غالبًا ما تسبب النعاس والخمول وضعف التركيز، بالإضافة إلى آثار جانبية أخرى مثل جفاف الفم وتشوش الرؤية.

            من أشهر أمثلتها: الديفينهيدرامين، الهيدروكسيزين، الكلورفينيرامين، والبروميثازين.

            ورغم فعاليتها في تخفيف الحكة وأعراض الحساسية، فإن تأثيرها المهدئ قد يعيق الأأنشطة اليومية مثل العمل أو القيادة.

            مضادات الهيستامين من الجيل الثاني

            تم تطوير مضادات الهيستامين من الجيل الثاني لتكون أكثر انتقائية وأقل قدرة على الوصول إلى الدماغ، مما يقلل بشكل كبير من تأثيرها المسبب للنعاس.

            من أشهر أمثلتها: السيتريزين، الليفوسيتريزين، اللوراتادين، الديسلوراتادين، الفيكسوفينادين، البيلاستين، والروباتادين.

            وبفضل فعاليتها الجيدة وقلة آثارها الجانبية، تُعد الخيار المفضل لعلاج الشرى المزمن والحساسية على المدى الطويل.

            أهم الفروقات بين الجيلين
            - النعاس والخمول: شائع في الجيل الأول، وأقل بكثير في الجيل الثاني.
            - مدة التأثير: غالبًا ما تكون أقصر في الجيل الأول وأطول في الجيل الثاني.
            - التأثير على الأنشطة اليومية: أكبر في الجيل الأول وأقل في الجيل الثاني.
            - التوصيات الطبية الحديثة: توصي الإرشادات العلاجية باستخدام مضادات الهيستامين من الجيل الثاني كخيار أول لعلاج معظم حالات الشرى المزمن.

            الخلاصة

            على الرغم من أن كلا الجيلين يثبط تأثير الهيستامين ويخفف أعراض الحساسية، فإن مضادات الهيستامين من الجيل الثاني توفر فعالية مماثلة مع آثار جانبية أقل وتحمل أفضل من قبل المرضى. لذلك، أصبحت الخيار العلاجي المفضل في معظم حالات الشرى والحساسية.
        """.trimIndent(),

        "Français" to """
            Les antihistaminiques comptent parmi les médicaments les plus utilisés pour traiter les allergies et l’urticaire. Bien que tous les antihistaminiques agissent en bloquant l’action de l’histamine, une substance chimique libérée par l’organisme lors des réactions allergiques, ils ne sont pas tous identiques. Ils sont généralement classés en antihistaminiques de première génération et de deuxième génération, chacun possédant des caractéristiques spécifiques.

            Les antihistaminiques de première génération

            Les antihistaminiques de première génération ont été développés plus tôt et traversent facilement la barrière hémato-encéphalique. Ils agissent donc également sur le système nerveux central, ce qui explique leurs effets sédatifs fréquents.

            Ils peuvent provoquer de la somnolence, une baisse de la concentration, ainsi que des effets secondaires anticholinergiques tels que la sécheresse buccale et les troubles de la vision.

            Parmi les exemples les plus connus figurent la diphénhydramine, l’hydroxyzine, la chlorphéniramine et la prométhazine.

            Bien qu’ils soient efficaces pour soulager les démangeaisons et les symptômes allergiques, leur effet sédatif peut interférer avec les activités quotidiennes, notamment le travail et la conduite.

            Les antihistaminiques de deuxième génération

            Les antihistaminiques de deuxième génération ont été conçus pour être plus sélectifs et pénétrer beaucoup moins dans le cerveau. Ils permettent ainsi de contrôler efficacement les symptômes allergiques et l’urticaire tout en provoquant beaucoup moins de somnolence.

            Parmi les principaux représentants de cette classe figurent la cétirizine, la lévocétirizine, la loratadine, la desloratadine, la fexofénadine, la bilastine et la rupatadine.

            Grâce à leur meilleur profil de tolérance et à leur faible effet sédatif, ils sont généralement privilégiés pour le traitement à long terme de l’urticaire chronique et des maladies allergiques.

            Principales différences entre les deux générations
            - Somnolence : fréquente avec les antihistaminiques de première génération, beaucoup plus rare avec ceux de deuxième génération.
            - Durée d’action : généralement plus courte pour la première génération et plus longue pour la deuxième génération.
            - Impact sur les activités quotidiennes : plus important avec la première génération, plus limité avec la deuxième génération.
            - Recommandations médicales : les antihistaminiques de deuxième génération sont recommandés comme traitement de première intention pour la plupart des patients atteints d’urticaire chronique.

            Conclusion

            Bien que les deux générations d’antihistaminiques soient efficaces pour bloquer l’action de l’histamine et soulager les symptômes allergiques, les antihistaminiques de deuxième génération offrent une efficacité comparable avec moins d’effets secondaires et une meilleure tolérance. C’est pourquoi ils constituent aujourd’hui le traitement de référence pour la majorité des patients souffrant d’urticaire et d’allergies.
        """.trimIndent(),

        "Español" to """
            Los antihistamínicos se encuentran entre los medicamentos más utilizados para tratar las alergias y la urticaria. Aunque todos los antihistamínicos actúan bloqueando la acción de la histamina, una sustancia química liberada por el organismo durante las reacciones alérgicas, no todos son iguales. Generalmente se clasifican en antihistamínicos de primera generación y de segunda generación, cada uno con características propias.

            Antihistamínicos de Primera Generación

            Los antihistamínicos de primera generación fueron desarrollados antes y atraviesan fácilmente la barrera hematoencefálica. Por ello, también afectan al sistema nervioso central y suelen causar somnolencia y sedación.

            Además, pueden provocar otros efectos secundarios, como sequedad de boca, visión borrosa y disminución de la concentración.

            Entre los ejemplos más conocidos se encuentran la difenhidramina, la hidroxicina, la clorfenimarina y la prometazina.

            Aunque son eficaces para aliviar el picor y los síntomas alérgicos, su efecto sedante puede interferir con las actividades diarias, especialmente con el trabajo y la conducción.

            Antihistamínicos de Segunda Generación

            Los antihistamínicos de segunda generación fueron diseñados para ser más selectivos y penetrar mucho menos en el cerebro. Como resultado, alivian eficazmente los síntomas de la alergia y la urticaria con un riesgo significativamente menor de somnolencia.

            Entre los principales ejemplos se incluyen la cetirizina, la levocetirizina, la loratadina, la desloratadina, la fexofenadina, la bilastina y la rupatadina.

            Gracias a su mejor perfil de seguridad y tolerabilidad, son la opción preferida para el tratamiento a largo plazo de la urticaria crónica y otras enfermedades alérgicas.

            Principales Diferencias entre Ambas Generaciones
            - Somnolencia: frecuente con los antihistamínicos de primera generación y mucho menos común con los de segunda generación.
            - Duración de acción: generalmente más corta en la primera generación y más prolongada en la segunda.
            - Impacto en las actividades diarias: mayor con la primera generación y menor con la segunda.
            - Recomendaciones médicas: los antihistamínicos de segunda generación son el tratamiento de primera elección para la mayoría de los pacientes con urticaria crónica.

            Conclusión

            Aunque ambas generaciones de antihistamínicos son eficaces para bloquear la acción de la histamina y aliviar los síntomas alérgicos, los antihistamínicos de segunda generación ofrecen una eficacia similar con menos efectos secundarios y una mejor tolerabilidad. Por esta razón, actualmente son la opción preferida para el tratamiento de la mayoría de los casos de urticaria y alergias.
        """.trimIndent(),

        "Deutsch" to """
            Antihistaminika gehören zu den am häufigsten verwendeten Medikamenten zur Behandlung von Allergien und Urtikaria (Nesselsucht). Obwohl alle Antihistaminika die Wirkung von Histamin blockieren – einer chemischen Substanz, die bei allergischen Reaktionen vom Körper freigesetzt wird –, sind sie nicht alle gleich. Sie werden im Allgemeinen in Antihistaminika der ersten und der zweiten Generation eingeteilt, die jeweils unterschiedliche Eigenschaften aufweisen.

            Antihistaminika der ersten Generation

            Antihistaminika der ersten Generation wurden früher entwickelt und können die Blut-Hirn-Schranke leicht überwinden. Dadurch wirken sie auch auf das zentrale Nervensystem und verursachen häufig Müdigkeit, Schläfrigkeit und Konzentrationsstörungen.

            Darüber hinaus können sie weitere Nebenwirkungen wie Mundtrockenheit, verschwommenes Sehen und andere anticholinerge Effekte hervorrufen.

            Zu den bekanntesten Vertretern dieser Gruppe gehören Diphenhydramin, Hydroxyzin, Chlorphenamin und Promethazin.

            Obwohl sie wirksam gegen Juckreiz und allergische Symptome sind, kann ihre beruhigende Wirkung den Alltag, die Arbeit und das Autofahren beeinträchtigen.

            Antihistaminika der zweiten Generation

            Antihistaminika der zweiten Generation wurden entwickelt, um gezielter zu wirken und deutlich weniger in das Gehirn einzudringen. Dadurch lindern sie Allergie- und Urtikariasymptome wirksam, ohne dabei in gleichem Maße Schläfrigkeit zu verursachen.

            Zu den wichtigsten Vertretern gehören Cetirizin, Levocetirizin, Loratadin, Desloratadin, Fexofenadin, Bilastin und Rupatadin.

            Aufgrund ihres günstigen Sicherheitsprofils und ihrer besseren Verträglichkeit gelten sie als bevorzugte Wahl für die langfristige Behandlung von chronischer Urtikaria und allergischen Erkrankungen.

            Die wichtigsten Unterschiede im Überblick
            - Schläfrigkeit: Häufig bei Antihistaminika der ersten Generation, deutlich seltener bei der zweiten Generation.
            - Wirkungsdauer: Meist kürzer bei der ersten Generation und länger bei der zweiten Generation.
            - Auswirkungen auf den Alltag: Stärker bei der ersten Generation, geringer bei der zweiten Generation.
            - Medizinische Empfehlungen: Antihistaminika der zweiten Generation werden als Erstlinientherapie für die meisten Patienten mit chronischer Urtikaria empfohlen.

            Fazit

            Obwohl beide Generationen von Antihistaminika die Wirkung von Histamin wirksam blockieren und allergische Symptome lindern, bieten Antihistaminika der zweiten Generation eine vergleichbare Wirksamkeit bei weniger Nebenwirkungen und besserer Verträglichkeit. Aus diesem Grund gelten sie heute als Standardtherapie für die meisten Patienten mit Urtikaria und Allergien.
        """.trimIndent(),

        "Italiano" to """
            Gli antistaminici sono tra i farmaci più utilizzati per il trattamento delle allergie e dell’orticaria. Sebbene tutti gli antistaminici agiscano bloccando l’azione dell’istamina, una sostanza chimica rilasciata dall’organismo durante le reazioni allergiche, non sono tutti uguali. Generalmente vengono classificati in antistaminici di prima generazione e di seconda generazione, ciascuno con caratteristiche specifiche.

            Antistaminici di Prima Generazione

            Gli antistaminici di prima generazione sono stati sviluppati per primi e attraversano facilmente la barriera ematoencefalica. Di conseguenza, agiscono anche sul sistema nervoso centrale e possono causare sonnolenza, sedazione e riduzione della concentrazione.

            Possono inoltre provocare altri effetti collaterali, come secchezza delle fauci, visione offuscata ed effetti anticolinergici.

            Tra gli esempi più noti vi sono la difenidramina, l’idrossizina, la clorfenamina e la prometazina.

            Sebbene siano efficaci nel ridurre il prurito e i sintomi allergici, il loro effetto sedativo può interferire con le normali attività quotidiane, compreso il lavoro e la guida.

            Antistaminici di Seconda Generazione

            Gli antistaminici di seconda generazione sono stati progettati per essere più selettivi e per penetrare molto meno nel cervello. Di conseguenza, alleviano efficacemente i sintomi dell’allergia e dell’orticaria con un rischio significativamente inferiore di sonnolenza.

            Tra i principali esempi figurano la cetirizina, la levocetirizina, la loratadina, la desloratadina, la fexofenadina, la bilastina e la rupatadina.

            Grazie al loro migliore profilo di sicurezza e tollerabilità, rappresentano la scelta preferita per il trattamento a lungo termine dell’orticaria cronica e delle malattie allergiche.

            Principali Differenze tra le Due Generazioni
            - Sonnolenza: comune con gli antistaminici di prima generazione, molto meno frequente con quelli di seconda generazione.
            - Durata d’azione: generalmente più breve nella prima generazione e più lunga nella seconda.
            - Impatto sulle attività quotidiane: maggiore con la prima generazione e minore con la seconda.
            - Raccomandazioni mediche: gli antistaminici di seconda generazione sono raccomandati come trattamento di prima linea per la maggior parte dei pazienti con orticaria cronica.

            Conclusione

            Sebbene entrambe le generazioni di antistaminici siano efficaci nel bloccare l’azione dell’istamina e nel controllare i sintomi allergici, gli antistaminici di seconda generazione offrono un’efficacia comparabile con meno effetti collaterali e una migliore tollerabilità. Per questo motivo, oggi rappresentano la scelta terapeutica preferita per la maggior parte dei pazienti affetti da orticaria e allergie.
        """.trimIndent()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Elegant Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                // Back Button in the top left
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .background(DarkCard, shape = CircleShape)
                        .size(40.dp)
                ) {
                    Text(
                        text = "←",
                        color = LightGray,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Centered Profile Title
                Text(
                    text = "👤 Your Profile",
                    color = SoftPurple,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Scrollable Main Section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Compact Personal Information Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Personal Information",
                            color = SoftPurple,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Name, Birth Date/Age, Sex on one single Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Name Input
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Name", color = MutedGray, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF737373),
                                    unfocusedTextColor = Color(0xFF737373),
                                    focusedBorderColor = SoftPurple,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface
                                ),
modifier = Modifier.weight(1.2f)
                            )

                            // Birth Date Field (Clickable Box opening DatePickerDialog)
                            Box(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .clickable {
                                        val currentBirthDate = try {
                                            if (birthDateInput.isNotEmpty()) LocalDate.parse(birthDateInput) else LocalDate.now().minusYears(25)
                                        } catch (e: Exception) {
                                            LocalDate.now().minusYears(25)
                                        }
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val selected = LocalDate.of(y, m + 1, d)
                                                birthDateInput = selected.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                            },
                                            currentBirthDate.year,
                                            currentBirthDate.monthValue - 1,
                                            currentBirthDate.dayOfMonth
                                        ).show()
                                    }
                            ) {
                                OutlinedTextField(
                                    value = if (computedAge.isNotEmpty()) computedAge else "Set Date",
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = false,
                                    label = {
                                        Text(
                                            text = "Age",
                                            color = MutedGray,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = Color(0xFF737373),
                                        disabledBorderColor = DarkBorder,
                                        disabledLabelColor = MutedGray,
                                        disabledContainerColor = DarkSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Gender toggle in the same line
                            Column(
                                modifier = Modifier.weight(1.1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Gender", color = LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    // Female checkbox choice
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable { sexInput = "F" }
                                            .padding(end = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = sexInput == "F",
                                            onCheckedChange = { if (it) sexInput = "F" },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = DarkGray,
                                                uncheckedColor = MutedGray,
                                                checkmarkColor = Color.Black
                                            ),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text("♀️", fontSize = 14.sp)
                                    }

                                    // Male checkbox choice
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { sexInput = "M" }
                                    ) {
                                        Checkbox(
                                            checked = sexInput == "M",
                                            onCheckedChange = { if (it) sexInput = "M" },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = DarkGray,
                                                uncheckedColor = MutedGray,
                                                checkmarkColor = Color.Black
                                            ),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text("♂️", fontSize = 14.sp)
                                    }
                                }
                            }
                        }


                    }
                }

                // Conditional Female Section (Female Body Rhythm)
                if (sexInput == "F") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Female Body Rhythm",
                                color = SoftPurple,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Pregnancy Selection
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Pregnant?", color = LightGray, fontSize = 14.sp)
                                Row {
                                    Box(
                                        modifier = Modifier
                                            .size(height = 36.dp, width = 64.dp)
                                            .background(
                                                color = if (pregnantInput == "Yes") DarkGray else DarkSurface,
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (pregnantInput == "Yes") DarkBorder else DarkBorder,
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                            .clickable { 
                                                pregnantInput = "Yes" 
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Yes",
                                            color = if (pregnantInput == "Yes") Color.Black else Color(0xFF737373),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(height = 36.dp, width = 64.dp)
                                            .background(
                                                color = if (pregnantInput == "No") DarkGray else DarkSurface,
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (pregnantInput == "No") DarkBorder else DarkBorder,
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                            .clickable { 
                                                pregnantInput = "No" 
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No",
                                            color = if (pregnantInput == "No") Color.Black else Color(0xFF737373),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Sub-Conditional pregnancy fields
                            if (pregnantInput == "Yes") {
                                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = monthInput,
                                            onValueChange = { input ->
                                                // Restrict input to 1-9 strictly
                                                if (input.isEmpty() || (input.length == 1 && input[0] in '1'..'9')) {
                                                    monthInput = input
                                                }
                                            },
                                            label = { Text("Pregnancy Month (1-9)", color = MutedGray) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color(0xFF737373),
                                                unfocusedTextColor = Color(0xFF737373),
                                                focusedBorderColor = SoftYellow,
                                                unfocusedBorderColor = DarkBorder,
                                                focusedContainerColor = DarkSurface,
                                                unfocusedContainerColor = DarkSurface
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        Button(
                                            onClick = {
                                                val current = monthInput.toIntOrNull()
                                                if (current == null) {
                                                    monthInput = "1"
                                                } else if (current < 9) {
                                                    monthInput = (current + 1).toString()
                                                } else {
                                                    showPregnancyOverDialog = true
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkGray),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(50.dp)
                                        ) {
                                            Text("+ Add", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.saveProfile(
                                                    name = nameInput.trim(),
                                                    birthDate = birthDateInput,
                                                    age = computedAge,
                                                    sex = sexInput,
                                                    pregnant = if (sexInput == "F") pregnantInput else "",
                                                    month = if (sexInput == "F" && pregnantInput == "Yes") monthInput.trim() else "",
                                                    diagnoses = selectedDiagnoses,
                                                    cortisoneName = "",
                                                    cortisoneMg = "",
                                                    onXolair = onXolairInput
                                                )
                                                Toast.makeText(context, "Pregnancy month saved!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF509729)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(50.dp)
                                        ) {
                                            Text("Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else if (pregnantInput == "No") {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(12.dp))

                                // Log & View Buttons Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { 
                                            cycleToEdit = null
                                            showLogDialog = true 
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkGray),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("➕ Log Cycle", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { showCyclesInMainScreen = !showCyclesInMainScreen },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (showCyclesInMainScreen) Color(0xFF333333) else DarkGray
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "📅 Cycle History",
                                            color = if (showCyclesInMainScreen) Color.White else Color.Black,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (showCyclesInMainScreen) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    val avgMetrics = remember(cycles) {
                                        try {
                                            val endedCycles = cycles.filter { it.endDate != null }
                                            val avgDuration = if (endedCycles.isNotEmpty()) {
                                                endedCycles.map { c ->
                                                    val start = LocalDate.parse(c.startDate)
                                                    val end = LocalDate.parse(c.endDate)
                                                    ChronoUnit.DAYS.between(start, end) + 1
                                                }.average()
                                            } else {
                                                null
                                            }

                                            val sortedStartDates = cycles.mapNotNull { c ->
                                                try { LocalDate.parse(c.startDate) } catch (e: Exception) { null }
                                            }.sorted()

                                            val avgLength = if (sortedStartDates.size >= 2) {
                                                val lengths = mutableListOf<Long>()
                                                for (i in 0 until sortedStartDates.size - 1) {
                                                    lengths.add(ChronoUnit.DAYS.between(sortedStartDates[i], sortedStartDates[i + 1]))
                                                }
                                                lengths.average()
                                            } else {
                                                null
                                            }

                                            Pair(avgDuration, avgLength)
                                        } catch (e: Exception) {
                                            Pair(null, null)
                                        }
                                    }

                                    // Header for the cycle logs
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Cycle History",
                                                color = SoftPurple,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val durationStr = avgMetrics.first?.let { "%.1f Days".format(it) } ?: "--"
                                            val lengthStr = avgMetrics.second?.let { "%.1f Days".format(it) } ?: "--"
                                            Text(
                                                text = "Avg Duration: $durationStr • Avg Cycle: every $lengthStr",
                                                color = MutedGray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }

                                        // Export button
                                        OutlinedButton(
                                            onClick = { performCyclesExport(context, cycles) },
                                            border = BorderStroke(1.dp, Color(0xFF737373)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.Transparent,
                                                contentColor = Color(0xFF737373)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Export", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // List container
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 420.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        if (cycles.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No cycles recorded.",
                                                    color = MutedGray,
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        } else {
                                            cycles.forEach { cycle ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                        .background(DarkSurface, shape = RoundedCornerShape(12.dp))
                                                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                                                        .padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(
                                                            text = "Start: ${formatCycleDate(cycle.startDate)}",
                                                            color = Color(0xFF737373),
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = "End: ${cycle.endDate?.let { formatCycleDate(it) } ?: "Ongoing (Active)"}",
                                                            color = if (cycle.endDate == null) SoftYellow else MutedGray,
                                                            fontSize = 11.sp
                                                        )
                                                    }

                                                    val durationText = getCycleDuration(cycle.startDate, cycle.endDate)
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                color = if (cycle.endDate == null) SoftYellow.copy(alpha = 0.15f) else DarkBorder,
                                                                shape = RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = durationText,
                                                            color = if (cycle.endDate == null) SoftYellow else PastelIceBlue,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(6.dp))

                                                    Text(
                                                        text = "Edit",
                                                        color = LinkBlue,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier
                                                            .clickable {
                                                                cycleToEdit = cycle
                                                                showLogDialog = true
                                                            }
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    Text(
                                                        text = "Clear",
                                                        color = AlertRed,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier
                                                            .clickable {
                                                                viewModel.deleteMenstruationCycle(cycle.id)
                                                                Toast.makeText(context, "Cycle deleted.", Toast.LENGTH_SHORT).show()
                                                            }
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Urticaria Diagnosis Card
                var showDiagnosisSelector by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDiagnosisSelector = !showDiagnosisSelector }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Urticaria Diagnosis",
                                color = SoftPurple,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (showDiagnosisSelector) "▲" else "▼",
                                color = SoftPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (showDiagnosisSelector) {
                            diagnosisOptions.forEach { diagnosis ->
                                val isChecked = selectedDiagnoses.contains(diagnosis)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedDiagnoses = if (isChecked) {
                                                selectedDiagnoses - diagnosis
                                            } else {
                                                selectedDiagnoses + diagnosis
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedDiagnoses = if (checked == true) {
                                                selectedDiagnoses + diagnosis
                                            } else {
                                                selectedDiagnoses - diagnosis
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = SoftPurple,
                                            uncheckedColor = MutedGray,
                                            checkmarkColor = Color.Black
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = diagnosis,
                                        color = Color(0xFF737373),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            if (selectedDiagnoses.isEmpty()) {
                                Text(
                                    text = "No diagnosis selected (Tap header to choose)",
                                    color = MutedGray,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                selectedDiagnoses.forEach { diagnosis ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(SoftPurple, shape = RoundedCornerShape(3.dp))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = diagnosis,
                                            color = Color(0xFF737373),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "On Xolair (or alt)",
                                color = Color(0xFF737373),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Yes Option
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onXolairInput = true }
                                ) {
                                    Checkbox(
                                        checked = onXolairInput,
                                        onCheckedChange = { if (it) onXolairInput = true },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = DarkGray,
                                            uncheckedColor = MutedGray,
                                            checkmarkColor = Color.Black
                                        )
)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Yes", color = if (onXolairInput) Color.Black else Color(0xFF737373), fontSize = 13.sp)
                                }

                                // No Option
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onXolairInput = false }
                                ) {
                                    Checkbox(
                                        checked = !onXolairInput,
                                        onCheckedChange = { if (it) onXolairInput = false },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = DarkGray,
                                            uncheckedColor = MutedGray,
                                            checkmarkColor = Color.Black
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("No", color = if (!onXolairInput) Color.Black else Color(0xFF737373), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // 3. Antihistamines Entry & List Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "My Antihistamines",
                            color = SoftPurple,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Saved Antihistamines List
                        if (antihistamines.isEmpty()) {
                            Text(
                                text = "No antihistamines added.",
                                color = MutedGray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        } else {
                            antihistamines.forEach { med ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(DarkSurface, RoundedCornerShape(8.dp))
                                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = med.name,
                                            color = Color(0xFF737373),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${med.mgs} mg" + if (med.generation.isNotEmpty()) " • ${if (med.generation == "1st") "1st generation" else "2nd generation"}" else "",
                                            color = Color(0xFF737373),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.toggleMainAntihistamine(med.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text(if (med.isMain) "⭐" else "☆", fontSize = 14.sp)
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Edit",
                                            color = LinkBlue,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable {
                                                    editingAntihistamineId = med.id
                                                    newMedName = med.name
                                                    newMedMgs = med.mgs
                                                    newMedGen = med.generation
                                                }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Clear",
                                            color = AlertRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable { viewModel.deleteProfileAntihistamine(med.id) }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // "+ Add" toggle row if form is not open
                        if (!showAddAntihistamineForm && editingAntihistamineId == null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAddAntihistamineForm = true }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "➕ Add",
                                    color = SoftPurple,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        AnimatedVisibility(visible = showAddAntihistamineForm || editingAntihistamineId != null) {
                            Column {
                                HorizontalDivider(color = DarkBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                                // Form to add antihistamine
                                Text(
                                    text = if (editingAntihistamineId != null) "Edit Antihistamine Details" else "Add Antihistamine",
                                    color = LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                    val query = newMedName.trim().lowercase()
                                    val suggestions = remember(query) {
                                        if (query.isEmpty()) emptyList() else {
                                            val list = MedicationDirectory.antihistamines
                                            fun normalize(s: String): String = s.replace("–", "-").replace("—", "-").trim().lowercase()
                                            val normalizedQuery = normalize(query)
                                            val exactMatches = list.filter { med ->
                                                val normalizedFull = normalize(med)
                                                val parts = med.split(" – ")
                                                val normalizedName = if (parts.isNotEmpty()) normalize(parts[0]) else ""
                                                normalizedFull == normalizedQuery || normalizedName == normalizedQuery
                                            }
                                            val startsWithMatches = list.filter { med ->
                                                if (exactMatches.contains(med)) return@filter false
                                                val normalizedFull = normalize(med)
                                                val parts = med.split(" – ")
                                                val normalizedName = if (parts.isNotEmpty()) normalize(parts[0]) else ""
                                                normalizedFull.startsWith(normalizedQuery) || normalizedName.startsWith(normalizedQuery)
                                            }
                                            val containsMatches = list.filter { med ->
                                                if (exactMatches.contains(med) || startsWithMatches.contains(med)) return@filter false
                                                val normalizedFull = normalize(med)
                                                normalizedFull.contains(normalizedQuery)
                                            }
                                            (exactMatches + startsWithMatches + containsMatches).take(8)
                                        }
                                    }
                                    
                                    LaunchedEffect(suggestions) {
                                        selectedIndex = -1
                                    }

                                    OutlinedTextField(
                                        value = newMedName,
                                        onValueChange = { 
                                            newMedName = it
                                            showAutocompleteSuggestions = true
                                        },
                                        label = { Text("Med Name", color = MutedGray, fontSize = 12.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color(0xFF737373),
                                            unfocusedTextColor = Color(0xFF737373),
                                            focusedBorderColor = SoftPurple,
                                            unfocusedBorderColor = DarkBorder,
                                            focusedContainerColor = DarkSurface,
                                            unfocusedContainerColor = DarkSurface
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { focusState ->
                                                showAutocompleteSuggestions = focusState.isFocused
                                            }
                                            .onKeyEvent { keyEvent ->
                                                if (showAutocompleteSuggestions && suggestions.isNotEmpty() && keyEvent.type == KeyEventType.KeyDown) {
                                                    when (keyEvent.key) {
                                                        Key.DirectionDown -> {
                                                            selectedIndex = (selectedIndex + 1) % suggestions.size
                                                            true
                                                        }
                                                        Key.DirectionUp -> {
                                                            selectedIndex = if (selectedIndex <= 0) suggestions.size - 1 else selectedIndex - 1
                                                            true
                                                        }
                                                        Key.Enter -> {
                                                            if (selectedIndex in suggestions.indices) {
                                                                val med = suggestions[selectedIndex]
                                                                newMedName = med
                                                                showAutocompleteSuggestions = false
                                                                if (med.contains("1st generation")) {
                                                                    newMedGen = "1st"
                                                                } else if (med.contains("2nd generation")) {
                                                                    newMedGen = "2nd"
                                                                }
                                                                true
                                                            } else {
                                                                false
                                                            }
                                                        }
                                                        else -> false
                                                    }
                                                } else {
                                                    false
                                                }
                                            }
                                    )
                                    
                                    if (showAutocompleteSuggestions && newMedName.trim().isNotEmpty()) {
                                        Popup(
                                            onDismissRequest = { showAutocompleteSuggestions = false },
                                            properties = PopupProperties(focusable = false)
                                        ) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp)
                                                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)),
                                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                                shape = RoundedCornerShape(8.dp),
                                                elevation = CardDefaults.cardElevation(8.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 240.dp)
                                                        .verticalScroll(rememberScrollState())
                                                ) {
                                                    if (suggestions.isEmpty()) {
                                                        Text(
                                                            text = "No medications found.\nNo matching medication found. You can enter it manually.",
                                                            color = MutedGray,
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.padding(12.dp)
                                                        )
                                                    } else {
                                                        suggestions.forEachIndexed { index, med ->
                                                            val isSelected = index == selectedIndex
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .background(if (isSelected) SoftPurple.copy(alpha = 0.2f) else Color.Transparent)
                                                                    .clickable {
                                                                        newMedName = med
                                                                        showAutocompleteSuggestions = false
                                                                        if (med.contains("1st generation")) {
                                                                            newMedGen = "1st"
                                                                        } else if (med.contains("2nd generation")) {
                                                                            newMedGen = "2nd"
                                                                        }
                                                                    }
                                                                    .padding(12.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                val lowercaseMed = med.lowercase()
                                                                if (lowercaseMed.contains(query)) {
                                                                    val parts = mutableListOf<Pair<String, Boolean>>()
                                                                    var searchIndex = 0
                                                                    while (searchIndex < med.length) {
                                                                        val idx = lowercaseMed.indexOf(query, searchIndex)
                                                                        if (idx == -1) {
                                                                            parts.add(med.substring(searchIndex) to false)
                                                                            break
                                                                        }
                                                                        if (idx > searchIndex) {
                                                                            parts.add(med.substring(searchIndex, idx) to false)
                                                                        }
                                                                        parts.add(med.substring(idx, idx + query.length) to true)
                                                                        searchIndex = idx + query.length
                                                                    }
                                                                    Text(
                                                                        text = buildAnnotatedString {
                                                                            parts.forEach { (part, isHigh) ->
                                                                                if (isHigh) {
                                                                                    withStyle(style = SpanStyle(color = SoftPurple, fontWeight = FontWeight.Bold)) {
                                                                                        append(part)
                                                                                    }
                                                                                } else {
                                                                                    withStyle(style = SpanStyle(color = Color.White)) {
                                                                                        append(part)
                                                                                    }
                                                                                }
                                                                            }
                                                                        },
                                                                        fontSize = 12.sp
                                                                    )
                                                                } else {
                                                                    Text(text = med, color = Color.White, fontSize = 12.sp)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    
                                                    HorizontalDivider(color = DarkBorder)
                                                    Text(
                                                        text = "Can't find your medication? Continue typing to enter it manually.",
                                                        color = PastelIceBlue.copy(alpha = 0.8f),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(10.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = newMedMgs,
                                    onValueChange = { newMedMgs = it },
                                    label = { Text("Dosage (mgs)", color = MutedGray, fontSize = 12.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color(0xFF737373),
                                        unfocusedTextColor = Color(0xFF737373),
                                        focusedBorderColor = SoftPurple,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedContainerColor = DarkSurface,
                                        unfocusedContainerColor = DarkSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                )

                                // Generation Checkboxes next to each other
                                generosityRow@
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            newMedGen = if (newMedGen == "1st") "" else "1st"
                                        }
                                    ) {
                                        Checkbox(
                                            checked = newMedGen == "1st",
                                            onCheckedChange = { checked ->
                                                newMedGen = if (checked == true) "1st" else ""
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = SoftPurple,
                                                uncheckedColor = MutedGray,
                                                checkmarkColor = Color.Black
                                            )
                                        )
                                        Text("1st generation", color = Color(0xFF737373), fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            newMedGen = if (newMedGen == "2nd") "" else "2nd"
                                        }
                                    ) {
                                        Checkbox(
                                            checked = newMedGen == "2nd",
                                            onCheckedChange = { checked ->
                                                newMedGen = if (checked == true) "2nd" else ""
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = SoftPurple,
                                                uncheckedColor = MutedGray,
                                                checkmarkColor = Color.Black
                                            )
                                        )
                                        Text("2nd generation", color = Color(0xFF737373), fontSize = 11.sp)
                                    }
                                }

                                // Informational clickable link
                                Text(
                                    text = "First-Generation vs Second-Generation Antihistamines: What's the Difference?",
                                    color = Color(0xFF737373),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier
                                        .padding(vertical = 12.dp)
                                        .clickable { showInfoDialog = true }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (editingAntihistamineId != null) {
                                        Button(
                                            onClick = {
                                                if (newMedName.trim().isEmpty()) {
                                                    Toast.makeText(context, "Please enter medicine name.", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (newMedMgs.trim().isEmpty() || newMedMgs.trim().toDoubleOrNull() == null) {
                                                    Toast.makeText(context, "Please enter a valid mgs dosage.", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                val existingIsMain = antihistamines.find { it.id == editingAntihistamineId }?.isMain ?: false
                                                viewModel.updateProfileAntihistamine(
                                                    id = editingAntihistamineId!!,
                                                    name = newMedName.trim(),
                                                    mgs = newMedMgs.trim(),
                                                    generation = newMedGen,
                                                    isMain = existingIsMain
                                                )
                                                editingAntihistamineId = null
                                                newMedName = ""
                                                newMedMgs = ""
                                                newMedGen = ""
                                                Toast.makeText(context, "Antihistamine updated.", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Text("Update Med", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = {
                                                editingAntihistamineId = null
                                                newMedName = ""
                                                newMedMgs = ""
                                                newMedGen = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Text("Cancel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (newMedName.trim().isEmpty()) {
                                                        Toast.makeText(context, "Please enter medicine name.", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    if (newMedMgs.trim().isEmpty() || newMedMgs.trim().toDoubleOrNull() == null) {
                                                        Toast.makeText(context, "Please enter a valid mgs dosage.", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    viewModel.addProfileAntihistamine(
                                                        name = newMedName.trim(),
                                                        mgs = newMedMgs.trim(),
                                                        generation = newMedGen
                                                    )
                                                    newMedName = ""
                                                    newMedMgs = ""
                                                    newMedGen = ""
                                                    showAddAntihistamineForm = false
                                                    Toast.makeText(context, "Antihistamine added.", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(36.dp)
                                            ) {
                                                Text("Add", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = {
                                                    showAddAntihistamineForm = false
                                                    newMedName = ""
                                                    newMedMgs = ""
                                                    newMedGen = ""
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(36.dp)
                                            ) {
                                                Text("Cancel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showReminderDialogForType = "ANTIHISTAMINE" }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔔 Create Reminder",
                                color = Color(0xFF509729),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Corticosteroids Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Corticosteroids",
                            color = SoftPurple,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Saved Cortisone List
                        if (profileCortisones.isEmpty()) {
                            Text(
                                text = "No Corticosteroids added.",
                                color = MutedGray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        } else {
                            profileCortisones.forEach { med ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(DarkSurface, RoundedCornerShape(8.dp))
                                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = med.name,
                                            color = Color(0xFF737373),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${med.mgs} mg",
                                            color = Color(0xFF737373),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Edit",
                                            color = LinkBlue,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable {
                                                    editingCortisoneId = med.id
                                                    newCortisoneName = med.name
                                                    newCortisoneMgs = med.mgs
                                                }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Clear",
                                            color = AlertRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable { viewModel.deleteProfileCortisone(med.id) }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // "+ Add" toggle row if form is not open
                        if (!showAddCortisoneForm && editingCortisoneId == null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAddCortisoneForm = true }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "➕ Add",
                                    color = SoftPurple,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        AnimatedVisibility(visible = showAddCortisoneForm || editingCortisoneId != null) {
                            Column {
                                HorizontalDivider(color = DarkBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                                // Form to add Cortisone
                                Text(
                                    text = if (editingCortisoneId != null) "Edit Corticosteroid Details" else "Add Corticosteroid",
                                    color = LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                OutlinedTextField(
                                    value = newCortisoneName,
                                    onValueChange = { newCortisoneName = it },
                                    label = { Text("Med Name (e.g. Prednisolone)", color = MutedGray, fontSize = 12.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color(0xFF737373),
                                        unfocusedTextColor = Color(0xFF737373),
                                        focusedBorderColor = SoftYellow,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedContainerColor = DarkSurface,
                                        unfocusedContainerColor = DarkSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                )

                                OutlinedTextField(
                                    value = newCortisoneMgs,
                                    onValueChange = { newCortisoneMgs = it },
                                    label = { Text("Dosage (mg)", color = MutedGray, fontSize = 12.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color(0xFF737373),
                                        unfocusedTextColor = Color(0xFF737373),
                                        focusedBorderColor = SoftYellow,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedContainerColor = DarkSurface,
                                        unfocusedContainerColor = DarkSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (editingCortisoneId != null) {
                                        Button(
                                            onClick = {
                                                if (newCortisoneName.trim().isEmpty()) {
                                                    Toast.makeText(context, "Please enter medicine name.", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (newCortisoneMgs.trim().isEmpty() || newCortisoneMgs.trim().toDoubleOrNull() == null) {
                                                    Toast.makeText(context, "Please enter a valid mgs dosage.", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                viewModel.updateProfileCortisone(
                                                    id = editingCortisoneId!!,
                                                    name = newCortisoneName.trim(),
                                                    mgs = newCortisoneMgs.trim()
                                                )
                                                editingCortisoneId = null
                                                newCortisoneName = ""
                                                newCortisoneMgs = ""
                                                Toast.makeText(context, "Cortisone/Steroid updated.", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Text("Update Med", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = {
                                                editingCortisoneId = null
                                                newCortisoneName = ""
                                                newCortisoneMgs = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Text("Cancel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (newCortisoneName.trim().isEmpty()) {
                                                        Toast.makeText(context, "Please enter medicine name.", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    if (newCortisoneMgs.trim().isEmpty() || newCortisoneMgs.trim().toDoubleOrNull() == null) {
                                                        Toast.makeText(context, "Please enter a valid mgs dosage.", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    viewModel.addProfileCortisone(newCortisoneName.trim(), newCortisoneMgs.trim())
                                                    newCortisoneName = ""
                                                    newCortisoneMgs = ""
                                                    showAddCortisoneForm = false
                                                    Toast.makeText(context, "Cortisone/Steroid added.", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(36.dp)
                                            ) {
                                                Text("Add", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = {
                                                    showAddCortisoneForm = false
                                                    newCortisoneName = ""
                                                    newCortisoneMgs = ""
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(36.dp)
                                            ) {
                                                Text("Cancel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showReminderDialogForType = "CORTICOSTEROID" }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔔 Create Reminder",
                                color = Color(0xFF509729),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4b. Other Medications Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Other Medications",
                            color = SoftPurple,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Local sub-tabs: Active / History
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedOtherMedTab = 0 }
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Active",
                                    color = if (selectedOtherMedTab == 0) SoftPurple else MutedGray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(if (selectedOtherMedTab == 0) SoftPurple else Color.Transparent)
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedOtherMedTab = 1 }
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "History",
                                    color = if (selectedOtherMedTab == 1) SoftPurple else MutedGray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(if (selectedOtherMedTab == 1) SoftPurple else Color.Transparent)
                                )
                            }
                        }

                        if (selectedOtherMedTab == 0) {
                            // Saved Other Medications List
                            if (profileOtherMedications.isEmpty()) {
                                Text(
                                    text = "No other medications added.",
                                    color = MutedGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            } else {
                                profileOtherMedications.forEach { med ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)),
                                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = med.name,
                                                    color = SoftPurple,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (med.medType.isNotEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(SoftPurple.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                            .border(1.dp, SoftPurple.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = med.medType,
                                                            color = SoftPurple,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "${med.mgs} mg • ${med.frequency}",
                                                color = Color(0xFF737373),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (med.condition.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "For: ${med.condition}",
                                                    color = Color(0xFF737373),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            if (med.startDate.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Started: ${med.startDate}",
                                                    color = Color(0xFF737373),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .border(1.dp, Color(0xFF509729), RoundedCornerShape(4.dp))
                                                        .clickable {
                                                            selectedMedForCourseCompletion = med
                                                            try {
                                                                if (med.startDate.isNotEmpty()) {
                                                                    courseStartDate = LocalDate.parse(med.startDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                                                } else {
                                                                    courseStartDate = LocalDate.now()
                                                                }
                                                            } catch (e: Exception) {
                                                                courseStartDate = LocalDate.now()
                                                            }
                                                            courseEndDate = LocalDate.now()
                                                            showCourseCompletionDialog = true
                                                        }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "Course Completed?",
                                                        color = Color(0xFF509729),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Edit",
                                                        color = LinkBlue,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier
                                                            .clickable {
                                                                editingOtherMedId = med.id
                                                                newOtherMedName = med.name
                                                                newOtherMedMgs = med.mgs
                                                                newOtherMedCondition = med.condition
                                                                newOtherMedFrequency = med.frequency
                                                                newOtherMedType = med.medType
                                                                newOtherMedStartDate = med.startDate
                                                            }
                                                            .padding(vertical = 4.dp)
                                                    )
                                                    Text(
                                                        text = "Clear",
                                                        color = AlertRed,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier
                                                            .clickable { viewModel.deleteProfileOtherMedication(med.id) }
                                                            .padding(vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // "+ Add" toggle row if form is not open
                            if (!showAddOtherMedForm && editingOtherMedId == null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            newOtherMedStartDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                            showAddOtherMedForm = true 
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "➕ Add",
                                        color = SoftPurple,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            AnimatedVisibility(visible = showAddOtherMedForm || editingOtherMedId != null) {
                                Column {
                                    HorizontalDivider(color = DarkBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                                    // Form to add Other Medication
                                    Text(
                                        text = if (editingOtherMedId != null) "Edit Medication Details" else "Add Medication",
                                        color = LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    OutlinedTextField(
                                        value = newOtherMedName,
                                        onValueChange = { newOtherMedName = it },
                                        label = { Text("Med Name (e.g. Montelukast)", color = MutedGray, fontSize = 12.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color(0xFF737373),
                                            unfocusedTextColor = Color(0xFF737373),
                                            focusedBorderColor = SoftYellow,
                                            unfocusedBorderColor = DarkBorder,
                                            focusedContainerColor = DarkSurface,
                                            unfocusedContainerColor = DarkSurface
                                        ),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    )

                                    // Med Type selector under Med Name
                                    val medTypeOptions = listOf(
                                        "Analgesics (Pain Relievers)",
                                        "Antihistamines",
                                        "Antibiotics",
                                        "Antivirals",
                                        "Antifungals",
                                        "Antiparasitics",
                                        "Anti-inflammatory Drugs",
                                        "Corticosteroids",
                                        "Immunosuppressants",
                                        "Immunomodulators",
                                        "Biologics",
                                        "Chemotherapy Drugs",
                                        "Hormonal Medications",
                                        "Contraceptives",
                                        "Fertility Medications",
                                        "Antidiabetic Medications",
                                        "Cardiovascular Medications",
                                        "Antihypertensives",
                                        "Antiarrhythmics",
                                        "Anticoagulants",
                                        "Antiplatelets",
                                        "Thrombolytics",
                                        "Cholesterol-Lowering Medications",
                                        "Diuretics",
                                        "Respiratory Medications",
                                        "Bronchodilators",
                                        "Asthma Medications",
                                        "COPD Medications",
                                        "Cough Medications",
                                        "Decongestants",
                                        "Gastrointestinal Medications",
                                        "Antacids",
                                        "Proton Pump Inhibitors (PPIs)",
                                        "H2-Receptor Blockers",
                                        "Antiemetics",
                                        "Antidiarrheals",
                                        "Laxatives",
                                        "Neurological Medications",
                                        "Antiepileptics",
                                        "Antiparkinsonian Medications",
                                        "Migraine Medications",
                                        "Psychiatric Medications",
                                        "Antidepressants",
                                        "Antipsychotics",
                                        "Anxiolytics",
                                        "Mood Stabilizers",
                                        "Sedatives",
                                        "Hypnotics (Sleep Medications)",
                                        "Stimulants",
                                        "Muscle Relaxants",
                                        "Anesthetics",
                                        "Local Anesthetics",
                                        "General Anesthetics",
                                        "Dermatological Medications",
                                        "Ophthalmic Medications",
                                        "Otic (Ear) Medications",
                                        "Urological Medications",
                                        "Erectile Dysfunction Medications",
                                        "Osteoporosis Medications",
                                        "Rheumatology Medications",
                                        "Gout Medications",
                                        "Thyroid Medications",
                                        "Vaccines",
                                        "Blood Products",
                                        "Enzyme Replacement Therapies",
                                        "Orphan Drugs",
                                        "Nutritional Supplements",
                                        "Vitamin Supplements",
                                        "Mineral Supplements",
                                        "Probiotics",
                                        "Emergency Medications (e.g., epinephrine)",
                                        "Leukotriene Receptor Antagonists",
                                        "Other"
                                    )

                                    var medTypeMenuExpanded by remember { mutableStateOf(false) }
                                    val filteredMedTypes = remember(newOtherMedType) {
                                        val cleanOptions = medTypeOptions.filter { it != "Other" }
                                        if (newOtherMedType.isEmpty()) {
                                            cleanOptions
                                        } else {
                                            cleanOptions.filter { it.contains(newOtherMedType, ignoreCase = true) }
                                        }
                                    }

                                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = newOtherMedType,
                                                onValueChange = {
                                                    newOtherMedType = it
                                                    medTypeMenuExpanded = true
                                                },
                                                label = { Text("Med Type", color = MutedGray, fontSize = 12.sp) },
                                                trailingIcon = {
                                                    Text(
                                                        text = "▼",
                                                        color = MutedGray,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier
                                                            .clickable { medTypeMenuExpanded = !medTypeMenuExpanded }
                                                            .padding(8.dp)
                                                    )
                                                },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color(0xFF737373),
                                                    unfocusedTextColor = Color(0xFF737373),
                                                    focusedBorderColor = SoftYellow,
                                                    unfocusedBorderColor = DarkBorder,
                                                    focusedContainerColor = DarkSurface,
                                                    unfocusedContainerColor = DarkSurface
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .onFocusChanged { focusState ->
                                                        if (focusState.isFocused) {
                                                            medTypeMenuExpanded = true
                                                        }
                                                    }
                                            )

                                            if (medTypeMenuExpanded && filteredMedTypes.isNotEmpty()) {
                                                DropdownMenu(
                                                    expanded = medTypeMenuExpanded,
                                                    onDismissRequest = { medTypeMenuExpanded = false },
                                                    properties = PopupProperties(focusable = false),
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.9f)
                                                        .heightIn(max = 280.dp)
                                                        .background(DarkSurface)
                                                        .border(1.dp, DarkBorder)
                                                ) {
                                                    filteredMedTypes.forEach { option ->
                                                        DropdownMenuItem(
                                                            text = { Text(option, color = Color(0xFF737373), fontSize = 12.sp) },
                                                            onClick = {
                                                                newOtherMedType = option
                                                                medTypeMenuExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Editable Start Date field
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                            .clickable {
                                                val now = LocalDate.now()
                                                val initialDate = try {
                                                    if (newOtherMedStartDate.isNotEmpty()) {
                                                        LocalDate.parse(newOtherMedStartDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                                    } else now
                                                } catch (e: Exception) {
                                                    now
                                                }
                                                DatePickerDialog(
                                                    context,
                                                    { _, y, m, d ->
                                                        newOtherMedStartDate = LocalDate.of(y, m + 1, d).format(DateTimeFormatter.ISO_LOCAL_DATE)
                                                    },
                                                    initialDate.year,
                                                    initialDate.monthValue - 1,
                                                    initialDate.dayOfMonth
                                                ).show()
                                            }
                                    ) {
                                        OutlinedTextField(
                                            value = newOtherMedStartDate,
                                            onValueChange = {},
                                            readOnly = true,
                                            enabled = false,
                                            label = { Text("Start Date", color = MutedGray, fontSize = 12.sp) },
                                            trailingIcon = {
                                                Text(
                                                    text = "📅",
                                                    fontSize = 14.sp,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                disabledTextColor = Color(0xFF737373),
                                                disabledBorderColor = DarkBorder,
                                                disabledLabelColor = MutedGray,
                                                disabledContainerColor = DarkSurface
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newOtherMedMgs,
                                            onValueChange = { newOtherMedMgs = it },
                                            label = { Text("Dosage (mg)", color = MutedGray, fontSize = 12.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color(0xFF737373),
                                                unfocusedTextColor = Color(0xFF737373),
                                                focusedBorderColor = SoftYellow,
                                                unfocusedBorderColor = DarkBorder,
                                                focusedContainerColor = DarkSurface,
                                                unfocusedContainerColor = DarkSurface
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        var frequencyMenuExpanded by remember { mutableStateOf(false) }
                                        val frequencyOptions = listOf(
                                            "Every 8 hours",
                                            "Every 12 hours",
                                            "Every 24 hours",
                                            "Every 2 days",
                                            "Every 3 days",
                                            "Once a week",
                                            "Biweekly",
                                            "Monthly",
                                            "Semimonthly",
                                            "Other"
                                        )

                                        Box(modifier = Modifier.weight(1f)) {
                                            OutlinedTextField(
                                                value = newOtherMedFrequency,
                                                onValueChange = { newOtherMedFrequency = it },
                                                label = { Text("Frequency", color = MutedGray, fontSize = 12.sp) },
                                                singleLine = true,
                                                trailingIcon = {
                                                    Text(
                                                        text = "▼",
                                                        color = MutedGray,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier
                                                            .clickable { frequencyMenuExpanded = !frequencyMenuExpanded }
                                                            .padding(8.dp)
                                                    )
                                                },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color(0xFF737373),
                                                    unfocusedTextColor = Color(0xFF737373),
                                                    focusedBorderColor = SoftYellow,
                                                    unfocusedBorderColor = DarkBorder,
                                                    focusedContainerColor = DarkSurface,
                                                    unfocusedContainerColor = DarkSurface
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            DropdownMenu(
                                                expanded = frequencyMenuExpanded,
                                                onDismissRequest = { frequencyMenuExpanded = false },
                                                modifier = Modifier.background(DarkSurface).border(1.dp, DarkBorder)
                                            ) {
                                                frequencyOptions.forEach { option ->
                                                    DropdownMenuItem(
                                                        text = { Text(option, color = Color(0xFF737373), fontSize = 12.sp) },
                                                        onClick = {
                                                            if (option == "Other") {
                                                                newOtherMedFrequency = ""
                                                            } else {
                                                                newOtherMedFrequency = option
                                                            }
                                                            frequencyMenuExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val conditionOptions = listOf(
                                        "Common cold",
                                        "Influenza (flu)",
                                        "COVID-19",
                                        "Allergic rhinitis (hay fever)",
                                        "Asthma",
                                        "Acute bronchitis",
                                        "Sinusitis",
                                        "Pharyngitis (sore throat)",
                                        "Tonsillitis",
                                        "Ear infection (otitis media)",
                                        "Gastroenteritis",
                                        "Food poisoning",
                                        "GERD (acid reflux)",
                                        "Urinary tract infection (UTI)",
                                        "Headache",
                                        "Migraine",
                                        "Fever (unspecified)",
                                        "Skin infection (bacterial)",
                                        "Fungal infection",
                                        "Conjunctivitis (pink eye)",
                                        "Chickenpox",
                                        "Shingles",
                                        "Eczema flare",
                                        "Urticaria (hives flare-up)",
                                        "Cuts / lacerations",
                                        "Abrasions (scrapes)",
                                        "Bruises (contusions)",
                                        "Sprains (includes ankle sprain, knee sprain)",
                                        "Strains (includes neck strain / whiplash)",
                                        "Fractures (broken bones)",
                                        "Dislocations",
                                        "Burns",
                                        "Concussion",
                                        "Back injury",
                                        "Finger/toe injury",
                                        "Sports injury (unspecified)",
                                        "Insect bites/stings",
                                        "Animal bites",
                                        "Others"
                                    )

                                    var conditionMenuExpanded by remember { mutableStateOf(false) }
                                    val filteredConditions = remember(newOtherMedCondition) {
                                        val cleanOptions = conditionOptions.filter { it != "Others" }
                                        if (newOtherMedCondition.isEmpty()) {
                                            cleanOptions
                                        } else {
                                            cleanOptions.filter { it.contains(newOtherMedCondition, ignoreCase = true) }
                                        }
                                    }

                                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = newOtherMedCondition,
                                                onValueChange = {
                                                    newOtherMedCondition = it
                                                    conditionMenuExpanded = true
                                                },
                                                label = { Text("Condition", color = MutedGray, fontSize = 12.sp) },
                                                trailingIcon = {
                                                    Text(
                                                        text = "▼",
                                                        color = MutedGray,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier
                                                            .clickable { conditionMenuExpanded = !conditionMenuExpanded }
                                                            .padding(8.dp)
                                                    )
                                                },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color(0xFF737373),
                                                    unfocusedTextColor = Color(0xFF737373),
                                                    focusedBorderColor = SoftYellow,
                                                    unfocusedBorderColor = DarkBorder,
                                                    focusedContainerColor = DarkSurface,
                                                    unfocusedContainerColor = DarkSurface
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .onFocusChanged { focusState ->
                                                        if (focusState.isFocused) {
                                                            conditionMenuExpanded = true
                                                        }
                                                    }
                                            )

                                            if (conditionMenuExpanded && filteredConditions.isNotEmpty()) {
                                                DropdownMenu(
                                                    expanded = conditionMenuExpanded,
                                                    onDismissRequest = { conditionMenuExpanded = false },
                                                    properties = PopupProperties(focusable = false),
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.9f)
                                                        .heightIn(max = 280.dp)
                                                        .background(DarkSurface)
                                                        .border(1.dp, DarkBorder)
                                                ) {
                                                    filteredConditions.forEach { option ->
                                                        DropdownMenuItem(
                                                            text = { Text(option, color = Color(0xFF737373), fontSize = 12.sp) },
                                                            onClick = {
                                                                newOtherMedCondition = option
                                                                conditionMenuExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (editingOtherMedId != null) {
                                            Button(
                                                onClick = {
                                                    if (newOtherMedName.trim().isEmpty()) {
                                                        Toast.makeText(context, "Please enter medicine name.", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    if (newOtherMedMgs.trim().isEmpty() || newOtherMedMgs.trim().toDoubleOrNull() == null) {
                                                        Toast.makeText(context, "Please enter a valid mgs dosage.", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    val finalStartDate = if (newOtherMedStartDate.trim().isEmpty()) {
                                                        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                                    } else {
                                                        newOtherMedStartDate.trim()
                                                    }
                                                    viewModel.updateProfileOtherMedication(
                                                        id = editingOtherMedId!!,
                                                        name = newOtherMedName.trim(),
                                                        mgs = newOtherMedMgs.trim(),
                                                        condition = newOtherMedCondition.trim(),
                                                        frequency = newOtherMedFrequency.trim(),
                                                        medType = newOtherMedType.trim(),
                                                        startDate = finalStartDate
                                                    )
                                                    editingOtherMedId = null
                                                    newOtherMedName = ""
                                                    newOtherMedMgs = ""
                                                    newOtherMedCondition = ""
                                                    newOtherMedFrequency = ""
                                                    newOtherMedType = ""
                                                    newOtherMedStartDate = ""
                                                    Toast.makeText(context, "Medication updated.", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(36.dp)
                                            ) {
                                                Text("Update Med", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = {
                                                    editingOtherMedId = null
                                                    newOtherMedName = ""
                                                    newOtherMedMgs = ""
                                                    newOtherMedCondition = ""
                                                    newOtherMedFrequency = ""
                                                    newOtherMedType = ""
                                                    newOtherMedStartDate = ""
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(36.dp)
                                            ) {
                                                Text("Cancel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        if (newOtherMedName.trim().isEmpty()) {
                                                            Toast.makeText(context, "Please enter medicine name.", Toast.LENGTH_SHORT).show()
                                                            return@Button
                                                        }
                                                        if (newOtherMedMgs.trim().isEmpty() || newOtherMedMgs.trim().toDoubleOrNull() == null) {
                                                            Toast.makeText(context, "Please enter a valid mgs dosage.", Toast.LENGTH_SHORT).show()
                                                            return@Button
                                                        }
                                                        val finalStartDate = if (newOtherMedStartDate.trim().isEmpty()) {
                                                            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                                        } else {
                                                            newOtherMedStartDate.trim()
                                                        }
                                                        viewModel.addProfileOtherMedication(
                                                            newOtherMedName.trim(),
                                                            newOtherMedMgs.trim(),
                                                            newOtherMedCondition.trim(),
                                                            newOtherMedFrequency.trim(),
                                                            newOtherMedType.trim(),
                                                            finalStartDate
                                                        )
                                                        newOtherMedName = ""
                                                        newOtherMedMgs = ""
                                                        newOtherMedCondition = ""
                                                        newOtherMedFrequency = ""
                                                        newOtherMedType = ""
                                                        newOtherMedStartDate = ""
                                                        showAddOtherMedForm = false
                                                        Toast.makeText(context, "Medication added.", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).height(36.dp)
                                                ) {
                                                    Text("Add", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = {
                                                        showAddOtherMedForm = false
                                                        newOtherMedName = ""
                                                        newOtherMedMgs = ""
                                                        newOtherMedCondition = ""
                                                        newOtherMedFrequency = ""
                                                        newOtherMedType = ""
                                                        newOtherMedStartDate = ""
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).height(36.dp)
                                                ) {
                                                    Text("Cancel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showReminderDialogForType = "OTHER" }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔔 Create Reminder",
                                    color = Color(0xFF509729),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // History Tab content
                            if (completedCourses.isEmpty()) {
                                Text(
                                    text = "No medication history available.",
                                    color = MutedGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            } else {
                                completedCourses.forEach { course ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(DarkSurface, RoundedCornerShape(8.dp))
                                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = course.name,
                                                color = Color(0xFF737373),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val details = buildString {
                                                if (course.medType.isNotEmpty()) {
                                                    append("${course.medType} • ")
                                                }
                                                append("${course.mgs} mg")
                                                if (course.frequency.isNotEmpty()) {
                                                    append(" • ${course.frequency}")
                                                }
                                                if (course.condition.isNotEmpty()) {
                                                    append(" • for ${course.condition}")
                                                }
                                            }
                                            Text(
                                                text = details,
                                                color = Color(0xFF737373),
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Taken: ${course.startDate} to ${course.endDate} (${course.durationDays} days)",
                                                color = Color(0xFF737373),
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            text = "Clear",
                                            color = AlertRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable { viewModel.deleteCompletedMedicationCourse(course.id) }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = { performMedicationHistoryExport(context, completedCourses) },
                                    border = BorderStroke(1.dp, Color(0xFF737373)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF737373),
                                        containerColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Text(
                                        text = "Export",
                                        color = Color(0xFF737373),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. Chronic Illnesses Card
                val chronicIllnesses by viewModel.profileChronicIllnesses.collectAsState()
                var newIllnessName by remember { mutableStateOf("") }
                var showIllnessSuggestions by remember { mutableStateOf(false) }
                var illnessSelectedIndex by remember { mutableIntStateOf(-1) }
                var showAddIllnessForm by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Chronic Illnesses",
                            color = SoftPurple,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // List of added chronic illnesses
                        if (chronicIllnesses.isEmpty()) {
                            Text(
                                text = "No chronic illnesses added.",
                                color = MutedGray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        } else {
                            chronicIllnesses.forEach { illness ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(DarkSurface, RoundedCornerShape(8.dp))
                                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = illness,
                                        color = Color(0xFF737373),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Clear",
                                        color = AlertRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { viewModel.deleteProfileChronicIllness(illness) }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // "+ Add" toggle row if form is not open
                        if (!showAddIllnessForm) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAddIllnessForm = true }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "➕ Add",
                                    color = SoftPurple,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        AnimatedVisibility(visible = showAddIllnessForm) {
                            Column {
                                HorizontalDivider(color = DarkBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                                Text(
                                    text = "Add Chronic Illness",
                                    color = LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                    val illnessQuery = newIllnessName.trim().lowercase()
                                    val illnessSuggestions = remember(illnessQuery) {
                                        if (illnessQuery.isEmpty()) emptyList() else {
                                            val list = ChronicIllnessesDirectory.illnesses
                                            fun normalize(s: String): String = s.trim().lowercase()
                                            val normalizedQuery = normalize(illnessQuery)
                                            val exactMatches = list.filter { ill ->
                                                normalize(ill) == normalizedQuery
                                            }
                                            val startsWithMatches = list.filter { ill ->
                                                if (exactMatches.contains(ill)) return@filter false
                                                normalize(ill).startsWith(normalizedQuery)
                                            }
                                            val containsMatches = list.filter { ill ->
                                                if (exactMatches.contains(ill) || startsWithMatches.contains(ill)) return@filter false
                                                normalize(ill).contains(normalizedQuery)
                                            }
                                            (exactMatches + startsWithMatches + containsMatches).take(8)
                                        }
                                    }

                                    LaunchedEffect(illnessSuggestions) {
                                        illnessSelectedIndex = -1
                                    }

                                    OutlinedTextField(
                                        value = newIllnessName,
                                        onValueChange = {
                                            newIllnessName = it
                                            showIllnessSuggestions = true
                                        },
                                        label = { Text("Illness Name", color = MutedGray, fontSize = 12.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color(0xFF737373),
                                            unfocusedTextColor = Color(0xFF737373),
                                            focusedBorderColor = SoftPurple,
                                            unfocusedBorderColor = DarkBorder,
                                            focusedContainerColor = DarkSurface,
                                            unfocusedContainerColor = DarkSurface
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { focusState ->
                                                showIllnessSuggestions = focusState.isFocused
                                            }
                                            .onKeyEvent { keyEvent ->
                                                if (showIllnessSuggestions && illnessSuggestions.isNotEmpty() && keyEvent.type == KeyEventType.KeyDown) {
                                                    when (keyEvent.key) {
                                                        Key.DirectionDown -> {
                                                            illnessSelectedIndex = (illnessSelectedIndex + 1) % illnessSuggestions.size
                                                            true
                                                        }
                                                        Key.DirectionUp -> {
                                                            illnessSelectedIndex = if (illnessSelectedIndex <= 0) illnessSuggestions.size - 1 else illnessSelectedIndex - 1
                                                            true
                                                        }
                                                        Key.Enter -> {
                                                            if (illnessSelectedIndex in illnessSuggestions.indices) {
                                                                newIllnessName = illnessSuggestions[illnessSelectedIndex]
                                                                showIllnessSuggestions = false
                                                                true
                                                            } else {
                                                                false
                                                            }
                                                        }
                                                        else -> false
                                                    }
                                                } else {
                                                    false
                                                }
                                            }
                                    )

                                    if (showIllnessSuggestions && newIllnessName.trim().isNotEmpty()) {
                                        Popup(
                                            onDismissRequest = { showIllnessSuggestions = false },
                                            properties = PopupProperties(focusable = false)
                                        ) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp)
                                                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)),
                                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                                shape = RoundedCornerShape(8.dp),
                                                elevation = CardDefaults.cardElevation(8.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 240.dp)
                                                        .verticalScroll(rememberScrollState())
                                                ) {
                                                    if (illnessSuggestions.isEmpty()) {
                                                        Text(
                                                            text = "No illnesses found.\nNo matching chronic illness found. You can enter it manually.",
                                                            color = MutedGray,
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.padding(12.dp)
                                                        )
                                                    } else {
                                                        illnessSuggestions.forEachIndexed { index, ill ->
                                                            val isSelected = index == illnessSelectedIndex
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .background(if (isSelected) SoftPurple.copy(alpha = 0.2f) else Color.Transparent)
                                                                    .clickable {
                                                                        newIllnessName = ill
                                                                        showIllnessSuggestions = false
                                                                    }
                                                                    .padding(12.dp)
                                                            ) {
                                                                Text(
                                                                    text = ill,
                                                                    color = Color(0xFF737373),
                                                                    fontSize = 12.sp
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (newIllnessName.trim().isEmpty()) {
                                                Toast.makeText(context, "Please enter an illness name.", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            viewModel.addProfileChronicIllness(newIllnessName.trim())
                                            newIllnessName = ""
                                            showIllnessSuggestions = false
                                            showAddIllnessForm = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Text("Add", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            showAddIllnessForm = false
                                            newIllnessName = ""
                                            showIllnessSuggestions = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, DarkBorder),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Text("Cancel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }


        // Course Completion Dialog
        if (showCourseCompletionDialog && selectedMedForCourseCompletion != null) {
            val med = selectedMedForCourseCompletion!!
            Dialog(onDismissRequest = { showCourseCompletionDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Medication Course Completed",
                            color = SoftPurple,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "${med.name} (${med.mgs} mg)",
                            color = Color(0xFF737373),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Start Date
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            courseStartDate = LocalDate.of(y, m + 1, d)
                                        },
                                        courseStartDate.year,
                                        courseStartDate.monthValue - 1,
                                        courseStartDate.dayOfMonth
                                    ).show()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Start Date", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = courseStartDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                    color = MutedGray,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        HorizontalDivider(color = DarkBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                        // End Date
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            courseEndDate = LocalDate.of(y, m + 1, d)
                                        },
                                        courseEndDate.year,
                                        courseEndDate.monthValue - 1,
                                        courseEndDate.dayOfMonth
                                    ).show()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("End Date", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = courseEndDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                    color = MutedGray,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        HorizontalDivider(color = DarkBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                        // Duration display (inclusive)
                        val durationDays = (ChronoUnit.DAYS.between(courseStartDate, courseEndDate) + 1).toInt()
                        Text(
                            text = "Duration: ${if (durationDays < 1) "Invalid Dates" else "$durationDays Days"}",
                            color = Color(0xFF509729),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showCourseCompletionDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    if (durationDays < 1) {
                                        Toast.makeText(context, "End date must be after or equal to start date.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.logCompletedMedicationCourse(
                                        medId = med.id,
                                        startDate = courseStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                        endDate = courseEndDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                        durationDays = durationDays
                                    )
                                    showCourseCompletionDialog = false
                                    selectedMedForCourseCompletion = null
                                    selectedOtherMedTab = 1
                                    Toast.makeText(context, "Medication course archived.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Archive", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        }
    }

    // Medication Reminder Dialog trigger
    if (showReminderDialogForType != null) {
        MedicationReminderDialog(
            medType = showReminderDialogForType!!,
            viewModel = viewModel,
            onDismiss = { showReminderDialogForType = null }
        )
    }

    // 1. Multilingual Comparison Dialog
    if (showInfoDialog) {
        Dialog(onDismissRequest = { showInfoDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .border(1.5.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header with close option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Antihistamine Guide",
                            color = SoftPurple,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Language Selector tab row
                    ScrollableTabRow(
                        selectedTabIndex = infoTexts.keys.toList().indexOf(selectedInfoLanguage),
                        containerColor = DarkSurface,
                        contentColor = PastelIceBlue,
                        edgePadding = 4.dp,
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        infoTexts.keys.forEach { lang ->
                            Tab(
                                selected = selectedInfoLanguage == lang,
                                onClick = { selectedInfoLanguage = lang },
                                text = { Text(lang, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrollable Info Text Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = infoTexts[selectedInfoLanguage] ?: "",
                            color = Color(0xFF737373),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = if (selectedInfoLanguage == "العربية") TextAlign.End else TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Got it 👍", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showPregnancyOverDialog) {
        Dialog(onDismissRequest = { showPregnancyOverDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.5.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pregnancy Complete?",
                        color = SoftPurple,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Since you have completed 9 months of pregnancy, is it over? Would you like to update your profile status?",
                        color = LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showPregnancyOverDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("No", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                showPregnancyOverDialog = false
                                pregnantInput = "No"
                                monthInput = ""
                                viewModel.saveProfile(
                                    name = nameInput.trim(),
                                    birthDate = birthDateInput,
                                    age = computedAge,
                                    sex = sexInput,
                                    pregnant = "No",
                                    month = "",
                                    diagnoses = selectedDiagnoses,
                                    cortisoneName = "",
                                    cortisoneMg = "",
                                    onXolair = onXolairInput
                                )
                                Toast.makeText(context, "Profile updated: Pregnancy over", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF509729)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Yes", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }


    // 3. New/Edit Cycle Dialog
    if (showLogDialog) {
        var startLocalDate by remember {
            mutableStateOf(
                cycleToEdit?.let { LocalDate.parse(it.startDate) } ?: LocalDate.now()
            )
        }
        var endLocalDate by remember {
            mutableStateOf(
                cycleToEdit?.endDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
            )
        }
        var hasEndDate by remember {
            mutableStateOf(cycleToEdit?.endDate != null)
        }

        Dialog(onDismissRequest = { showLogDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, DarkBorder, RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (cycleToEdit == null) "Log Menstruation Cycle" else "Edit Cycle Record",
                        color = SoftPurple,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Start Date selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        startLocalDate = LocalDate.of(y, m + 1, d)
                                    },
                                    startLocalDate.year,
                                    startLocalDate.monthValue - 1,
                                    startLocalDate.dayOfMonth
                                ).show()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Start Date", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = startLocalDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                color = MutedGray,
                                fontSize = 13.sp
                            )
                        }
                    }

                    HorizontalDivider(color = DarkBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // End Date Selection Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Has End Date?", color = LightGray, fontSize = 13.sp)
                        Switch(
                            checked = hasEndDate,
                            onCheckedChange = { hasEndDate = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PastelIceBlue,
                                checkedTrackColor = DarkBorder,
                                uncheckedThumbColor = MutedGray,
                                uncheckedTrackColor = DarkSurface
                            )
                        )
                    }

                    if (hasEndDate) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            endLocalDate = LocalDate.of(y, m + 1, d)
                                        },
                                        endLocalDate.year,
                                        endLocalDate.monthValue - 1,
                                        endLocalDate.dayOfMonth
                                    ).show()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("End Date", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = endLocalDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                    color = MutedGray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "This cycle is currently ongoing. You can edit it to add an end date later.",
                            color = MutedGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dialog Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { showLogDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Text("Cancel", color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val startStr = startLocalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                val endStr = if (hasEndDate) endLocalDate.format(DateTimeFormatter.ISO_LOCAL_DATE) else null

                                // Validate date range
                                if (hasEndDate && endLocalDate.isBefore(startLocalDate)) {
                                    Toast.makeText(context, "End date cannot be prior to start date.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                if (cycleToEdit == null) {
                                    viewModel.addMenstruationCycle(startStr, endStr)
                                    Toast.makeText(context, "Cycle logged successfully.", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.updateMenstruationCycle(cycleToEdit!!.id, startStr, endStr)
                                    Toast.makeText(context, "Cycle record updated.", Toast.LENGTH_SHORT).show()
                                }
                                showLogDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        ) {
                            Text("Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Helper date formatter for UI presentation
private fun formatCycleDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    } catch (e: Exception) {
        dateStr
    }
}

// Helper cycle duration calculator
private fun getCycleDuration(startStr: String, endStr: String?): String {
    if (endStr == null) return "Ongoing"
    return try {
        val start = LocalDate.parse(startStr)
        val end = LocalDate.parse(endStr)
        val days = ChronoUnit.DAYS.between(start, end) + 1
        if (days < 1) "Invalid" else "$days Days"
    } catch (e: Exception) {
        "Error"
    }
}

// CSV exporter share chooser trigger
private fun performCyclesExport(context: android.content.Context, cycles: List<MenstruationCycle>) {
    if (cycles.isEmpty()) {
        Toast.makeText(context, "No cycle logs available to export.", Toast.LENGTH_SHORT).show()
        return
    }

    val csv = StringBuilder()
    csv.append("Start Date,End Date,Duration (Days)\n")
    cycles.forEach { c ->
        val duration = getCycleDuration(c.startDate, c.endDate)
        csv.append("\"${c.startDate}\",\"${c.endDate ?: "Ongoing"}\",\"$duration\"\n")
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Urticare Menstruation Cycles Export")
        putExtra(Intent.EXTRA_TEXT, csv.toString())
    }
    context.startActivity(Intent.createChooser(shareIntent, "Export Cycle Records"))
}

@Composable
fun MedicationReminderDialog(
    medType: String, // "ANTIHISTAMINE", "CORTICOSTEROID", "OTHER"
    viewModel: TrackerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val reminders by viewModel.profileReminders.collectAsState()
    val filteredReminders = remember(reminders, medType) { reminders.filter { it.medType == medType } }

    val antihistamines by viewModel.profileAntihistamines.collectAsState()
    val corticosteroids by viewModel.profileCortisones.collectAsState()
    val otherMeds by viewModel.profileOtherMedications.collectAsState()

    val savedMeds = remember(medType, antihistamines, corticosteroids, otherMeds) {
        when (medType) {
            "ANTIHISTAMINE" -> antihistamines.map { it.id to "${it.name} (${it.mgs} mg)" }
            "CORTICOSTEROID" -> corticosteroids.map { it.id to "${it.name} (${it.mgs} mg)" }
            else -> otherMeds.map { it.id to "${it.name} (${it.mgs} mg)" }
        }
    }

    val typeLabel = when (medType) {
        "ANTIHISTAMINE" -> "Antihistamine"
        "CORTICOSTEROID" -> "Corticosteroid"
        else -> "Other Medication"
    }

    // New reminder fields state
    var selectedMedId by remember(savedMeds) { mutableStateOf(savedMeds.firstOrNull()?.first ?: "") }
    var selectedScheduleType by remember { mutableStateOf("DAILY") } // "DAILY", "WEEKLY", "INTERVAL"

    // Time choice
    var selectedHour by remember { mutableIntStateOf(12) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var timeText by remember { mutableStateOf("12:00") }

    // Weekly days (1-7 for Mon-Sun)
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }

    // Interval hours
    var intervalHoursText by remember { mutableStateOf("8") }

    // Dropdown visibility
    var showMedDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.5.dp, DarkBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$typeLabel Reminders",
                        color = SoftPurple,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("❌", color = Color.White, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Current Reminders List
                    Text(
                        text = "Active Reminders",
                        color = MutedGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (filteredReminders.isEmpty()) {
                        Text(
                            text = "No reminders set.",
                            color = MutedGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        filteredReminders.forEach { reminder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(DarkSurface, RoundedCornerShape(8.dp))
                                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reminder.medName,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${reminder.medMgs} mg",
                                        color = PastelIceBlue,
                                        fontSize = 11.sp
                                    )

                                    val scheduleDetails = when (reminder.scheduleType) {
                                        "DAILY" -> "Daily at ${reminder.timeOfDay}"
                                        "WEEKLY" -> {
                                            val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                            val daysList = reminder.daysOfWeek.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
                                            val dayStr = if (daysList.size == 7) "Every day" else daysList.map { dayNames[it - 1] }.joinToString(", ")
                                            "Weekly on $dayStr at ${reminder.timeOfDay}"
                                        }
                                        "INTERVAL" -> "Every ${reminder.intervalHours} hours"
                                        else -> ""
                                    }
                                    Text(
                                        text = scheduleDetails,
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = reminder.isEnabled,
                                        onCheckedChange = { checked ->
                                            viewModel.toggleProfileReminder(reminder.id, checked, context)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = SoftPurple,
                                            uncheckedThumbColor = MutedGray,
                                            uncheckedTrackColor = DarkSurface
                                        ),
                                        modifier = Modifier.graphicsLayer(scaleX = 0.85f, scaleY = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Clear",
                                        color = AlertRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { viewModel.deleteProfileReminder(reminder.id, context) }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = DarkBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // 2. Add New Reminder Section
                    Text(
                        text = "Create Reminder",
                        color = Color(0xFF509729),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (savedMeds.isEmpty()) {
                        Text(
                            text = "You must enter medications in your Profile card first before setting reminders.",
                            color = CoralPink,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        // Med Selector
                        val selectedMedLabel = savedMeds.find { it.first == selectedMedId }?.second ?: "Select Medication"

                        Text("Select Medication", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(8.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                .clickable { showMedDropdown = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedMedLabel, color = Color.White, fontSize = 12.sp)
                                Text("▼", color = MutedGray, fontSize = 10.sp)
                            }
                            DropdownMenu(
                                expanded = showMedDropdown,
                                onDismissRequest = { showMedDropdown = false },
                                modifier = Modifier.background(DarkSurface).border(1.dp, DarkBorder)
                            ) {
                                savedMeds.forEach { med ->
                                    DropdownMenuItem(
                                        text = { Text(med.second, color = Color.White, fontSize = 12.sp) },
                                        onClick = {
                                            selectedMedId = med.first
                                            showMedDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Schedule Type chips
                        Text("Schedule Type", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("DAILY" to "Daily", "WEEKLY" to "Weekly Days", "INTERVAL" to "Every X Hours").forEach { (type, label) ->
                                val isSelected = selectedScheduleType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) SoftPurple else DarkSurface,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) SoftPurple else DarkBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedScheduleType = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Schedule specifics
                        when (selectedScheduleType) {
                            "DAILY" -> {
                                Text("Reminder Time", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkSurface, RoundedCornerShape(8.dp))
                                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            TimePickerDialog(
                                                context,
                                                { _, h, m ->
                                                    selectedHour = h
                                                    selectedMinute = m
                                                    timeText = String.format("%02d:%02d", h, m)
                                                },
                                                selectedHour,
                                                selectedMinute,
                                                true
                                            ).show()
                                        }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(timeText, color = Color.White, fontSize = 12.sp)
                                        Text("🕒", fontSize = 14.sp)
                                    }
                                }
                            }
                            "WEEKLY" -> {
                                Text("Reminder Time", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkSurface, RoundedCornerShape(8.dp))
                                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            TimePickerDialog(
                                                context,
                                                { _, h, m ->
                                                    selectedHour = h
                                                    selectedMinute = m
                                                    timeText = String.format("%02d:%02d", h, m)
                                                },
                                                selectedHour,
                                                selectedMinute,
                                                true
                                            ).show()
                                        }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(timeText, color = Color.White, fontSize = 12.sp)
                                        Text("🕒", fontSize = 14.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Days of the Week", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val daysLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                                    for (dayVal in 1..7) {
                                        val isSelected = selectedDays.contains(dayVal)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (isSelected) PastelIceBlue else DarkSurface,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) PastelIceBlue else DarkBorder,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .clickable {
                                                    selectedDays = if (isSelected) {
                                                        selectedDays - dayVal
                                                    } else {
                                                        selectedDays + dayVal
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                daysLabels[dayVal - 1],
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            "INTERVAL" -> {
                                Text("Interval (Hours)", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                                OutlinedTextField(
                                    value = intervalHoursText,
                                    onValueChange = { intervalHoursText = it.filter { char -> char.isDigit() } },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SoftPurple,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedContainerColor = DarkSurface,
                                        unfocusedContainerColor = DarkSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                val selectedMed = savedMeds.find { it.first == selectedMedId }
                                if (selectedMed == null) {
                                    Toast.makeText(context, "Please select a medication.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val medName = selectedMed.second.substringBefore(" (")
                                val medMgs = selectedMed.second.substringAfter("(").substringBefore(" mg)")

                                if (selectedScheduleType == "WEEKLY" && selectedDays.isEmpty()) {
                                    Toast.makeText(context, "Please select at least one day.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val intervalVal = intervalHoursText.toIntOrNull() ?: 8
                                if (selectedScheduleType == "INTERVAL" && intervalVal <= 0) {
                                    Toast.makeText(context, "Please enter a valid interval in hours.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val daysStr = selectedDays.sorted().joinToString(",")

                                val reminder = MedicationReminder(
                                    id = java.util.UUID.randomUUID().toString(),
                                    medId = selectedMedId,
                                    medName = medName,
                                    medMgs = medMgs,
                                    medType = medType,
                                    scheduleType = selectedScheduleType,
                                    timeOfDay = timeText,
                                    daysOfWeek = daysStr,
                                    intervalHours = intervalVal,
                                    isEnabled = true
                                )

                                viewModel.addProfileReminder(reminder, context)
                                Toast.makeText(context, "Reminder scheduled", Toast.LENGTH_SHORT).show()

                                // Reset form state
                                selectedDays = emptySet()
                                intervalHoursText = "8"
                                timeText = "12:00"
                                selectedHour = 12
                                selectedMinute = 0
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Text("Schedule Reminder", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun performMedicationHistoryExport(context: android.content.Context, history: List<CompletedMedicationCourse>) {
    if (history.isEmpty()) {
        android.widget.Toast.makeText(context, "No medication history available to export.", android.widget.Toast.LENGTH_SHORT).show()
        return
    }

    val csv = java.lang.StringBuilder()
    csv.append("Medication Name,Med Type,Dosage,Condition,Frequency,Start Date,End Date,Duration (Days)\n")
    history.forEach { item ->
        csv.append("\"${item.name}\",\"${item.medType}\",\"${item.mgs} mg\",\"${item.condition}\",\"${item.frequency}\",\"${item.startDate}\",\"${item.endDate}\",\"${item.durationDays}\"\n")
    }

    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, "Urticare Medication History Export")
        putExtra(android.content.Intent.EXTRA_TEXT, csv.toString())
    }
    context.startActivity(android.content.Intent.createChooser(shareIntent, "Export Medication History"))
}


package com.example.urticare20.model

enum class EntryType {
    FLARE_UP,
    ANTIHISTAMINE,
    CORTISONE,
     XOLAIR_150,
    XOLAIR_300,
    ALTERNATIVE
}

data class LogEntry(
    val id: String,
    val timestamp: String, // ISO 8601 Extended Format
    val type: EntryType,
    val metadata: String? = null // Optional details such as custom medication name and mg
)

data class SummaryMetrics(
    val last24Hours: Int,
    val last48Hours: Int,
    val last72Hours: Int,
    val currentMonthCount: Int
)

data class MenstruationCycle(
    val id: String,
    val startDate: String, // format: yyyy-MM-dd
    val endDate: String? = null // format: yyyy-MM-dd (optional, ongoing if null)
)

data class ProfileAntihistamine(
    val id: String,
    val name: String,
    val mgs: String,
    val generation: String, // "1st", "2nd", or ""
    val isMain: Boolean = false
)

data class ProfileCortisone(
    val id: String,
    val name: String,
    val mgs: String
)

data class ProfileOtherMedication(
    val id: String,
    val name: String,
    val mgs: String,
    val condition: String = "",
    val frequency: String = "",
    val medType: String = "",
    val startDate: String = ""
)

data class CompletedMedicationCourse(
    val id: String,
    val name: String,
    val mgs: String,
    val condition: String = "",
    val frequency: String = "",
    val startDate: String,
    val endDate: String,
    val durationDays: Int,
    val medType: String = ""
)


data class MedicationReminder(
    val id: String,
    val medId: String,
    val medName: String,
    val medMgs: String,
    val medType: String, // "ANTIHISTAMINE", "CORTICOSTEROID", "OTHER"
    val scheduleType: String, // "DAILY", "WEEKLY", "INTERVAL"
    val timeOfDay: String, // "HH:mm" (for DAILY / WEEKLY)
    val daysOfWeek: String, // Comma-separated list (e.g. "1,3,5" for Mon, Wed, Fri where Monday=1, ..., Sunday=7)
    val intervalHours: Int, // (for INTERVAL)
    val isEnabled: Boolean = true
)


object MedicationDirectory {
    val antihistamines = listOf(
        "Benadryl – 1st generation",
        "Atarax – 1st generation",
        "Phenergan – 1st generation",
        "Chlor-Trimeton – 1st generation",
        "Clemastine – 1st generation",
        "Unisom – 1st generation",
        "Periactin – 1st generation",
        "Ketotifen – 1st generation",
        "Brompheniramine – 1st generation",
        "Triprolidine – 1st generation",
        "Dimetapp – 1st generation",
        "Pheniramine – 1st generation",
        "Diphen – 1st generation",
        "Chlorphen – 1st generation",
        "Allerfin – 1st generation",
        "Anallerge – 1st generation",
        "Broncholar – 1st generation",
        "Histop – 1st generation",
        "Claritin – 2nd generation",
        "Loratadine – 2nd generation",
        "Desloratadine – 2nd generation",
        "Cetirizine – 2nd generation",
        "Levocetirizine – 2nd generation",
        "Zyrtec – 2nd generation",
        "Xyzal – 2nd generation",
        "Telefast – 2nd generation",
        "Telfast – 2nd generation",
        "Fexofenadine – 2nd generation",
        "Evastine – 2nd generation",
        "Astin – 2nd generation",
        "Ebastine – 2nd generation",
        "Bilastine – 2nd generation",
        "Rupafin – 2nd generation",
        "Rupatadine – 2nd generation",
        "Mizolastine – 2nd generation",
        "Allegra – 2nd generation",
        "Aerius – 2nd generation",
        "Azelex – 2nd generation",
        "Astelin – 2nd generation",
        "Levohistam – 2nd generation",
        "Quzyttir – 2nd generation",
        "Mosedin – 2nd generation",
        "Lacetirizine – 2nd generation",
        "Histazine – 2nd generation",
        "Aller-Tec – 2nd generation",
        "Allere – 2nd generation",
        "Allear – 2nd generation",
        "Allergstop – 2nd generation",
        "Levcet – 2nd generation",
        "L-Cet – 2nd generation",
        "Ebastine-Borg – 2nd generation",
        "Lastlerge – 2nd generation"
    )
}

object ChronicIllnessesDirectory {
    val illnesses = listOf(
        "Type 1 Diabetes",
        "Type 2 Diabetes",
        "Hypertension",
        "Coronary Artery Disease",
        "Heart Failure",
        "Atrial Fibrillation",
        "Peripheral Artery Disease",
        "Rheumatic Heart Disease",
        "Congenital Heart Disease",
        "Hypothyroidism",
        "Hyperthyroidism",
        "Hashimoto's Thyroiditis",
        "Graves' Disease",
        "Metabolic Syndrome",
        "Polycystic Ovary Syndrome (PCOS)",
        "Osteoporosis",
        "Asthma",
        "Chronic Obstructive Pulmonary Disease (COPD)",
        "Bronchiectasis",
        "Interstitial Lung Disease",
        "Pulmonary Fibrosis",
        "Cystic Fibrosis",
        "Obstructive Sleep Apnea",
        "Epilepsy",
        "Parkinson's Disease",
        "Alzheimer's Disease",
        "Multiple Sclerosis",
        "Migraine",
        "Cerebral Palsy",
        "Motor Neuron Disease",
        "Rheumatoid Arthritis",
        "Systemic Lupus Erythematosus (Lupus)",
        "Psoriatic Arthritis",
        "Ankylosing Spondylitis",
        "Sjögren Syndrome",
        "Systemic Sclerosis (Scleroderma)",
        "Vasculitis",
        "Crohn's Disease",
        "Ulcerative Colitis",
        "Celiac Disease",
        "Gastroesophageal Reflux Disease (GERD)",
        "Irritable Bowel Syndrome (IBS)",
        "Chronic Pancreatitis",
        "Chronic Kidney Disease (CKD)",
        "Polycystic Kidney Disease",
        "Interstitial Cystitis",
        "Neurogenic Bladder",
        "Nonalcoholic Fatty Liver Disease (NAFLD)",
        "Metabolic Dysfunction-Associated Steatotic Liver Disease (MASLD)",
        "Cirrhosis",
        "Autoimmune Hepatitis",
        "Primary Biliary Cholangitis",
        "Psoriasis",
        "Atopic Dermatitis (Eczema)",
        "Chronic Urticaria",
        "Hidradenitis Suppurativa",
        "Vitiligo",
        "Rosacea",
        "Major Depressive Disorder",
        "Generalized Anxiety Disorder (GAD)",
        "Bipolar Disorder",
        "Obsessive-Compulsive Disorder (OCD)",
        "Post-Traumatic Stress Disorder (PTSD)",
        "Schizophrenia",
        "Fibromyalgia",
        "Osteoarthritis",
        "Myalgic Encephalomyelitis / Chronic Fatigue Syndrome (ME/CFS)",
        "Chronic Back Pain",
        "Temporomandibular Disorder (TMD/TMJ)",
        "Human Immunodeficiency Virus (HIV) Infection",
        "Chronic Hepatitis B",
        "Chronic Hepatitis C",
        "Tuberculosis",
        "Breast Cancer",
        "Prostate Cancer",
        "Chronic Lymphocytic Leukemia (CLL)",
        "Multiple Myeloma",
        "Endometriosis",
        "Adenomyosis",
        "Chronic Pelvic Pain Syndrome",
        "Glaucoma",
        "Age-Related Macular Degeneration",
        "Retinitis Pigmentosa",
        "Ménière's Disease",
        "Chronic Sinusitis",
        "Chronic Rhinitis",
        "Gout",
        "Thalassemia",
        "Sickle Cell Disease",
        "Hemophilia",
        "Sarcoidosis",
        "Behçet's Disease",
        "Myasthenia Gravis",
        "Ehlers-Danlos Syndrome",
        "Marfan Syndrome",
        "Duchenne Muscular Dystrophy",
        "Becker Muscular Dystrophy",
        "Autism Spectrum Disorder",
        "Attention-Deficit/Hyperactivity Disorder (ADHD)",
        "Tourette Syndrome",
        "Chronic Venous Insufficiency",
        "Lymphedema",
        "Chronic Constipation",
        "Gastroparesis",
        "Achalasia",
        "Chronic Recurrent Pancreatitis",
        "Chronic Lyme Disease (controversial diagnosis)",
        "Chronic Pain Syndrome",
        "Complex Regional Pain Syndrome (CRPS)",
        "Trigeminal Neuralgia",
        "Chronic Daily Headache",
        "Cluster Headache",
        "Chronic Insomnia",
        "Narcolepsy",
        "Restless Legs Syndrome",
        "Chronic Anemia",
        "Primary Immunodeficiency Disorders",
        "Common Variable Immunodeficiency (CVID)",
        "Pemphigus Vulgaris",
        "Bullous Pemphigoid",
        "Alopecia Areata",
        "Lichen Planus",
        "Chronic Pruritus",
        "Keratoconus",
        "Chronic Dry Eye Disease",
        "Chronic Recurrent Urinary Tract Infection",
        "Overactive Bladder",
        "Chronic Prostatitis / Chronic Pelvic Pain Syndrome",
        "Chronic Graft-versus-Host Disease",
        "Chronic Rejection after Organ Transplant",
        "Long COVID (Post-COVID Condition)"
    )
}

object PotentialReasonsDirectory {
    val categories = mapOf(
        "Psychological & Neuro-Immune Triggers" to listOf(
            "Idiopathic Flare-Up (Spontaneous)",
            "Psychological stress",
            "The vicious \"stress-itch\" loop",
            "Illness",
            "Hive Flare-Up"
        ),
        "Dietary & Consumptions" to listOf(
            "Food Trigger: Spicy Food / Spices",
            "Food Trigger: Seafood",
            "Food Trigger: Shellfish",
            "Food Trigger: Peanuts / Treenuts (Nuts)",
            "Food Trigger: Diary (Animals Milk)",
            "Food Trigger: Plant-Based Milks",
            "Food Trigger: Aged & Fermented Foods",
            "Food Trigger: Vinegar & Pickles",
            "Food Trigger: Processed and Cured Meats",
            "Vegetables: Leafy (Lettuce, spinach, kale, cabbage)",
            "Vegetables: Cruciferous (Broccoli, radish, cauliflower, brussels sprouts)",
            "Vegetables: Root (Carrot, beetroot, turnip)",
            "Vegetables: Tuber (Potato, sweet potato, yam)",
            "Vegetables: Bulb (Onion, garlic, fennel)",
            "Vegetables: Stem (Celery, asparagus)",
            "Vegetables: Legumes (Peas, green beans, edamamae, broad beans)",
            "Vegetables: Mushrooms/Fungi",
            "Sea Vegetables",
            "Vegetables: Other",
            "Fruits: Drupes (Coconut, peach, plum, cherry, mango, olive)",
            "Fruits: Berries (Grape, blueberry, kiwi, banana, tomato)",
            "Fruits: Citrus (Lemon, orange, tangerine, grapefruit)",
            "Fruits: Pomes (Apple, pear, quince)",
            "Fruits: Melons (Watermelon, cantaloupe, pumpkin, cucumber)",
            "Fruit: Aggregate (Strawberry, raspberry, blackberry)",
            "Fruit: Tropical (Papaya, guava, passion fruit, dragon fruit, mangosteen)",
            "Fruit: Other",
            "Food Trigger: Stimulants and Additives",
            "Food Trigger: Chocolate / Cocoa",
            "Food Trigger: Poultry / Eggs",
            "Food Trigger: Beef",
            "Food Trigger: Chevon/Mutton",
            "Food Trigger: Pork",
            "Food Trigger: Wheat",
            "Food Trigger: Sesame",
            "Food Trigger: Honey",
            "Food Trigger: Soy Products",
            "Food Trigger: Plant-based oils",
            "Food Trigger: Molasses",
            "Raw Food",
            "Black Coffee",
            "Instant Coffee",
            "Coffee w/Milk",
            "Black Tea",
            "Fruit Tea",
            "Herbal Tea",
            "Soda",
            "Energy Drinks",
            "Alcohol: Beer",
            "Alcohol: Wine",
            "Alcohol: Spirits",
            "Alcohol: Mead",
            "Alcohol: Liqueurs",
            "Alcohol: Cider",
            "Non-Alcoholic Malt Beverages",
            "Boxed Juice",
            "Powdered/Smoothie Additions",
            "Malt Beverages",
            "Smoke: Cigarettes/Cigars/Vapes",
            "Smoke: Sheesha/Hookah",
            "Smoke: Cannabis Sativa"
        ),
        "Everyday External Physical Triggers" to listOf(
            "Ambient heat and overheating (including hot rooms or hot, scalding showers)",
            "Skin friction",
            "Pressure points (such as tight waistbands, restrictive clothing, or heavy straps)",
            "Sleep deprivation",
            "Exhaustion/Fatigue",
            "Dehydration"
        ),
        "Medication Intervals & Tracking Disruption" to listOf(
            "Missed Antihestamine Dosage",
            "Medication Change",
            "Xolair (or alt.) routine dosage temporary side effects flare-up",
            "Xolair (or alt) effect wearing off",
            "Medicine: Antibiotics",
            "Medicine: NSAIDs & Salicylates",
            "Medicine: Anti-inflammatory",
            "Medicine: ACE Inhibitors (Blood Pressure)",
            "Medicine: Herbal & Botanical",
            "Hormonal Inputs: Oral contraceptives",
            "Opioids & Narcotics: Morphine, Codeine, Meperidine, Fentanyl",
            "Vitamin: Water-Soluble Vitamins (Vitamin B-complex)",
            "Vitamin: Fat-Soluble Vitamins (vit A, D, C K)",
            "Vitamin: Amino Acids & Proteins",
            "Vitamin: Herbal & Botanical",
            "Vitamin: Minerals",
            "Vitamin: Essential Fatty Acids (Omega-3 & 6, DHA)",
            "Vitamin: Probiotics & Prebiotics",
            "Vitamin: Other Supplements"
        ),
        "Hormonal Triggers (Menstrual Cycle & Pregnancy)" to listOf(
            "Menstrual cycle phases and fluctuations",
            "Pregnancy-induced hormonal shifts",
            "Postpartum hormonal adjustments"
        )
    )
}





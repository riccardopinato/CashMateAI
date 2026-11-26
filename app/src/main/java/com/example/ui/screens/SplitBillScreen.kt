package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.SplitBill
import com.example.domain.model.SplitParticipant
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillScreen(
    viewModel: FinanceViewModel,
    onPremiumUpgradeRequired: (com.example.premium.PremiumFeature) -> Unit = {}
) {
    val splitBills by viewModel.splitBills.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("New") } // "New" or "History"

    // NEW SPLIT STATE
    var title by remember { mutableStateOf("") }
    var totalAmountStr by remember { mutableStateOf("") }
    val totalAmount = totalAmountStr.toDoubleOrNull() ?: 0.0

    var divisionType by remember { mutableStateOf("EQUAL") } // EQUAL, PERCENT, EXACT

    // List of participant names and their custom values
    var newParticipantName by remember { mutableStateOf("") }
    var newParticipantValue by remember { mutableStateOf("") }
    val tempParticipants = remember { mutableStateListOf<Pair<String, Double>>() }

    // Calculate calculated shares based on active division type
    val computedParticipants: List<SplitParticipant> = remember(tempParticipants.size, totalAmount, divisionType, tempParticipants.map { it.second }) {
        val count = tempParticipants.size
        if (count == 0) return@remember emptyList()

        when (divisionType) {
            "EQUAL" -> {
                val share = totalAmount / count
                tempParticipants.map { (name, _) ->
                    SplitParticipant(name, 1.0, share)
                }
            }
            "PERCENT" -> {
                tempParticipants.map { (name, percent) ->
                    val share = totalAmount * (percent / 100.0)
                    SplitParticipant(name, percent, share)
                }
            }
            "EXACT" -> {
                tempParticipants.map { (name, exactVal) ->
                    SplitParticipant(name, exactVal, exactVal)
                }
            }
            else -> emptyList()
        }
    }

    // Check mathematical validation
    val isValidSum = remember(computedParticipants, totalAmount, divisionType) {
        if (computedParticipants.isEmpty() || totalAmount <= 0.0) return@remember false
        when (divisionType) {
            "EQUAL" -> true
            "PERCENT" -> {
                val sumPercent = computedParticipants.sumOf { it.value }
                Math.abs(sumPercent - 100.0) < 0.01
            }
            "EXACT" -> {
                val sumExact = computedParticipants.sumOf { it.value }
                Math.abs(sumExact - totalAmount) < 0.01
            }
            else -> false
        }
    }

    Scaffold(
        containerColor = BackgroundWarm,
        topBar = {
            TopAppBar(
                title = { Text("Split Bill", fontWeight = FontWeight.Black, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWarm)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Section Tabs (Nuovo / Storico)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceElevation)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (activeTab == "New") EmeraldPrimary.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { activeTab = "New" }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nuovo Split",
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == "New") EmeraldPrimary else TextSecondary,
                        fontSize = 14.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (activeTab == "History") EmeraldPrimary.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { activeTab = "History" }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Storico Split",
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == "History") EmeraldPrimary else TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Crossfade(
                targetState = activeTab,
                animationSpec = tween(350),
                label = "tabTransition",
                modifier = Modifier.weight(1f)
            ) { tab ->
                if (tab == "New") {
                    // NEW SPLIT ENTRY SCREEN
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // 1 HERO CARD
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            border = BorderStroke(
                                1.2.dp, 
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00E676).copy(alpha = 0.35f),
                                        Color.White.copy(alpha = 0.04f)
                                    )
                                )
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .shadow(
                                    elevation = 14.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    clip = false,
                                    ambientColor = Color(0xFF00E676).copy(alpha = 0.22f),
                                    spotColor = Color(0xFF00E676).copy(alpha = 0.22f)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                SurfaceWhite,
                                                Color(0xFF0C1B14)
                                            )
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                // Dynamic background glowing circle
                                Canvas(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 24.dp, y = (-24).dp)
                                ) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0xFF00E676).copy(alpha = 0.16f), Color.Transparent),
                                            radius = size.width
                                        )
                                    )
                                }

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (title.isNotBlank()) title.uppercase(Locale.getDefault()) else "NUOVA SPESA CONDIVISA",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            letterSpacing = 1.2.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        )

                                        val methodLabel = when (divisionType) {
                                            "EQUAL" -> "Uguale"
                                            "PERCENT" -> "Percentuale"
                                            "EXACT" -> "Importo Esatto"
                                            else -> "Uguale"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(100))
                                                .background(Color(0xFF00E676).copy(alpha = 0.14f))
                                                .border(BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.25f)), RoundedCornerShape(100))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = methodLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00E676)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = String.format(Locale.getDefault(), "%,.2f %s", totalAmount, currency),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = (-0.5).sp
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Group,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (tempParticipants.isEmpty()) "Nessun partecipante" else "${tempParticipants.size} partecipanti",
                                                fontSize = 13.sp,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        val avgQuota = if (tempParticipants.isNotEmpty()) totalAmount / tempParticipants.size else 0.0
                                        Text(
                                            text = "Quota media: ${String.format(Locale.getDefault(), "%,.2f %s", avgQuota, currency)}",
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // 2 DETTAGLI CONTO (Premium Fields)
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    "DETTAGLI SPESA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.2.sp
                                )

                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("Nome Spesa / Conto") },
                                    placeholder = { Text("es. Cena Sushi, Regalo Compleanno") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                                        focusedLabelColor = EmeraldPrimary,
                                        unfocusedLabelColor = TextSecondary,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = SurfaceWhite,
                                        unfocusedContainerColor = SurfaceWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = totalAmountStr,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.toDoubleOrNull() != null || input == ".") {
                                            totalAmountStr = input
                                        }
                                    },
                                    label = { Text("Importo Totale ($currency)") },
                                    placeholder = { Text("es. 124.00") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                                        focusedLabelColor = EmeraldPrimary,
                                        unfocusedLabelColor = TextSecondary,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = SurfaceWhite,
                                        unfocusedContainerColor = SurfaceWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // 3 METODO DI DIVISIONE (Interactive Cards)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "METODO DI DIVISIONE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )

                            DivisionMethodCard(
                                isSelected = divisionType == "EQUAL",
                                title = "Uguale",
                                description = "Tutti pagano la stessa cifra",
                                icon = Icons.Default.Group,
                                onClick = { divisionType = "EQUAL" }
                            )

                            DivisionMethodCard(
                                isSelected = divisionType == "PERCENT",
                                title = "Percentuale",
                                description = "Dividi usando percentuali",
                                icon = Icons.Default.Percent,
                                onClick = { divisionType = "PERCENT" }
                            )

                            DivisionMethodCard(
                                isSelected = divisionType == "EXACT",
                                title = "Importo Esatto",
                                description = "Ogni persona paga un importo personalizzato",
                                icon = Icons.Default.AttachMoney,
                                onClick = { divisionType = "EXACT" }
                            )
                        }

                        // 4 PARTECIPANTI ADD FORM
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    "AGGIUNGI PARTECIPANTE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.2.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newParticipantName,
                                        onValueChange = { newParticipantName = it },
                                        label = { Text("Nome") },
                                        placeholder = { Text("es. Luca") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(20.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldPrimary,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                                            focusedLabelColor = EmeraldPrimary,
                                            unfocusedLabelColor = TextSecondary,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = SurfaceWhite,
                                            unfocusedContainerColor = SurfaceWhite
                                        ),
                                        modifier = Modifier.weight(1.5f)
                                    )

                                    if (divisionType != "EQUAL") {
                                        val fieldLabel = if (divisionType == "PERCENT") "%" else currency
                                        OutlinedTextField(
                                            value = newParticipantValue,
                                            onValueChange = { newParticipantValue = it },
                                            label = { Text(fieldLabel) },
                                            placeholder = { Text(if (divisionType == "PERCENT") "25" else "50") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(20.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EmeraldPrimary,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                                                focusedLabelColor = EmeraldPrimary,
                                                unfocusedLabelColor = TextSecondary,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedContainerColor = SurfaceWhite,
                                                unfocusedContainerColor = SurfaceWhite
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    val addInteractionSource = remember { MutableInteractionSource() }
                                    val addPressed by addInteractionSource.collectIsPressedAsState()
                                    val addScale by animateFloatAsState(
                                        targetValue = if (addPressed) 0.88f else 1f,
                                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                                        label = "addScale"
                                    )

                                    Button(
                                        onClick = {
                                            if (newParticipantName.isBlank()) return@Button
                                            val value = if (divisionType != "EQUAL") {
                                                newParticipantValue.toDoubleOrNull() ?: 0.0
                                            } else {
                                                1.0
                                            }
                                            tempParticipants.add(newParticipantName to value)
                                            newParticipantName = ""
                                            newParticipantValue = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        shape = RoundedCornerShape(20.dp),
                                        interactionSource = addInteractionSource,
                                        modifier = Modifier
                                            .height(56.dp)
                                            .width(56.dp)
                                            .graphicsLayer(scaleX = addScale, scaleY = addScale)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Aggiungi", tint = Color.Black)
                                    }
                                }

                                // 4 & 5 PARTECIPANTI LIST & QUOTE LIVE
                                if (tempParticipants.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        "PARTECIPANTI INSERITI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        letterSpacing = 1.2.sp
                                    )

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        computedParticipants.forEachIndexed { idx, p ->
                                            val rawValue = tempParticipants[idx].second
                                            ParticipantCard(
                                                name = p.name,
                                                shareAmount = p.shareAmount,
                                                currency = currency,
                                                divisionValue = rawValue,
                                                divisionType = divisionType,
                                                onDelete = { tempParticipants.removeAt(idx) },
                                                index = idx
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 6 RIEPILOGO (Summary Card)
                        if (computedParticipants.isNotEmpty()) {
                            SplitSummaryCard(
                                totalAmount = totalAmount,
                                participantCount = tempParticipants.size,
                                divisionType = divisionType,
                                currency = currency,
                                isValid = isValidSum
                            )
                        }

                        // Validation Warnings Banner
                        val errorMsg = when {
                            title.isBlank() -> "Il nome della spesa/conto è obbligatorio."
                            totalAmount <= 0.0 -> "L'importo totale deve essere maggiore di zero."
                            tempParticipants.size < 2 -> "Lo split richiede almeno 2 partecipanti."
                            divisionType == "PERCENT" && !isValidSum -> {
                                val sumPercent = computedParticipants.sumOf { it.value }
                                "La somma delle percentuali deve essere esattamente 100% (attuale: ${String.format(Locale.getDefault(), "%,.1f", sumPercent)}%)"
                            }
                            divisionType == "EXACT" && !isValidSum -> {
                                val sumCalculated = computedParticipants.sumOf { p -> p.shareAmount }
                                "La somma delle quote (${String.format(Locale.getDefault(), "%,.2f", sumCalculated)} $currency) deve corrispondere esattamente al totale ($totalAmount $currency)"
                            }
                            else -> null
                        }

                        if (errorMsg != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(errorMsg, fontSize = 12.sp, color = ExpenseRed, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        // Save Button
                        val isButtonEnabled = errorMsg == null
                        val saveInteractionSource = remember { MutableInteractionSource() }
                        val savePressed by saveInteractionSource.collectIsPressedAsState()
                        val saveScale by animateFloatAsState(
                            targetValue = if (savePressed) 0.95f else 1f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                            label = "saveScale"
                        )

                        Button(
                            onClick = {
                                if (!isButtonEnabled) return@Button
                                if (!com.example.premium.PremiumManager.canUseFeature(com.example.premium.PremiumFeature.UNLIMITED_SPLITS, currentCount = splitBills.size)) {
                                    onPremiumUpgradeRequired(com.example.premium.PremiumFeature.UNLIMITED_SPLITS)
                                    return@Button
                                }
                                viewModel.addSplitBill(
                                    title = title,
                                    totalAmount = totalAmount,
                                    divisionType = divisionType,
                                    participants = computedParticipants
                                )
                                // Reset form and navigate to History
                                title = ""
                                totalAmountStr = ""
                                tempParticipants.clear()
                                activeTab = "History"
                             },
                            enabled = isButtonEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                disabledContainerColor = Color.White.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            interactionSource = saveInteractionSource,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .graphicsLayer(scaleX = saveScale, scaleY = saveScale)
                                .testTag("save_split_bill_button")
                        ) {
                            Text(
                                "Salva Split",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isButtonEnabled) Color.Black else TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                } else {
                    // 7 STORICO SPLIT TIMELINE
                    if (splitBills.isEmpty()) {
                        // 8 EMPTY STATE
                        EmptySplitHistoryState(
                            onAction = { activeTab = "New" }
                        )
                    } else {
                        val groupedSplitBills = remember(splitBills) {
                            splitBills.groupBy { getSplitGroupHeader(it.dateMillis) }
                        }

                        LazyColumn(
                            modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            groupedSplitBills.forEach { (dateHeader, bills) ->
                                item {
                                    Text(
                                        text = dateHeader,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        letterSpacing = 1.2.sp,
                                        modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 4.dp)
                                    )
                                }

                                items(bills) { bill ->
                                    val participants = viewModel.parseParticipants(bill.participantsJson)
                                    TimelineSplitBillItem(
                                        bill = bill,
                                        currency = currency,
                                        participants = participants,
                                        onDelete = { viewModel.deleteSplitBill(bill.id) }
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3 SELECTION CARD COMPOSABLE
@Composable
fun DivisionMethodCard(
    isSelected: Boolean,
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 450f),
        label = "cardScale"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) EmeraldPrimary.copy(alpha = 0.08f) else SurfaceWhite
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) EmeraldPrimary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) EmeraldPrimary else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) EmeraldPrimary else TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selezionato",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 4 PARTECIPANTE CARD
@Composable
fun ParticipantCard(
    name: String,
    shareAmount: Double,
    currency: String,
    divisionValue: Double,
    divisionType: String,
    onDelete: () -> Unit,
    index: Int
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)) +
                slideInHorizontally(initialOffsetX = { -40 }, animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)) +
                expandVertically(animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shaded Glowing Avatar with initials
                val avatarColor = remember(name) { getSplitAvatarColor(name) }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(avatarColor, avatarColor.copy(alpha = 0.8f))
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val detailLabel = when (divisionType) {
                        "PERCENT" -> "Quota: ${String.format(Locale.getDefault(), "%,.1f", divisionValue)}%"
                        "EXACT" -> "Quota fissa"
                        else -> "Quota uguale"
                    }
                    Text(
                        text = detailLabel,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                // Computed Quote
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%,.2f %s", shareAmount, currency),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E676)
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.White.copy(alpha = 0.03f), CircleShape)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Elimina",
                        tint = ExpenseRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// 6 RIEPILOGO SUMMARY CARD
@Composable
fun SplitSummaryCard(
    totalAmount: Double,
    participantCount: Int,
    divisionType: String,
    currency: String,
    isValid: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevation),
        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RIEPILOGO CONTO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp
                )
                // Validity status indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(if (isValid) Color(0xFF00E676).copy(alpha = 0.1f) else ExpenseRed.copy(alpha = 0.1f))
                        .border(BorderStroke(1.dp, if (isValid) Color(0xFF00E676).copy(alpha = 0.25f) else ExpenseRed.copy(alpha = 0.25f)), RoundedCornerShape(100))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isValid) "VALIDO" else "NON UNITO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isValid) Color(0xFF00E676) else ExpenseRed
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Premium Bento Grid arrangement
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left box: Total
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .background(SurfaceWhite, RoundedCornerShape(16.dp))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Totale Spesa", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%,.2f %s", totalAmount, currency),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // Right box: Participants
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .background(SurfaceWhite, RoundedCornerShape(16.dp))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Partecipanti", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(14.dp))
                            Text(
                                text = "$participantCount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left bottom box: Method
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceWhite, RoundedCornerShape(16.dp))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    val methodLabel = when (divisionType) {
                        "EQUAL" -> "Uguale"
                        "PERCENT" -> "Percentuale"
                        "EXACT" -> "Esatto"
                        else -> "Uguale"
                    }
                    Column {
                        Text("Metodo", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = methodLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                    }
                }

                // Right bottom box: Avg share
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceWhite, RoundedCornerShape(16.dp))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    val avgQuota = if (participantCount > 0) totalAmount / participantCount else 0.0
                    Column {
                        Text("Quota Media", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%,.2f %s", avgQuota, currency),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// 7 TIMELINE HISTORIC ITEM COMPOSABLE
@Composable
fun TimelineSplitBillItem(
    bill: SplitBill,
    currency: String,
    participants: List<SplitParticipant>,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val emoji = remember(bill.title) { getSplitEmoji(bill.title) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 8.dp)
    ) {
        // Left Column for Timeline Node & Vertical line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            // Circle node
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceWhite)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 20.sp)
            }

            // Vertical line
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f, fill = false)
                    .heightIn(min = 40.dp)
                    .background(Color.White.copy(alpha = 0.05f))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content Card on the right
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bill.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(bill.dateMillis))
                        Text(
                            text = "$dateFormatted • ${participants.size} partecipanti",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.getDefault(), "%,.2f %s", bill.totalAmount, currency),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldPrimary
                        )

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Elimina",
                                tint = ExpenseRed.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(bottom = 8.dp))
                        participants.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val color = remember(p.name) { getSplitAvatarColor(p.name) }
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(color.copy(alpha = 0.15f))
                                            .border(BorderStroke(1.dp, color), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = p.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(p.name, fontSize = 13.sp, color = TextPrimary)
                                }
                                Text(
                                    text = String.format(Locale.getDefault(), "%,.2f %s", p.shareAmount, currency),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 8 EMPTY HISTORY STATE
@Composable
fun EmptySplitHistoryState(
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Large circular glowing icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.08f))
                    .border(BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.15f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GroupWork,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Nessuno split creato",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Crea il tuo primo conto condiviso per dividere le spese in modo equo con gli amici.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(100),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Crea Split",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// HELPERS
private fun getSplitAvatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFF3B82F6), // Blue
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444), // Red
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFF14B8A6), // Teal
        Color(0xFF06B6D4)  // Cyan
    )
    if (name.isEmpty()) return colors[0]
    val hash = name.hashCode().let { if (it < 0) -it else it }
    return colors[hash % colors.size]
}

private fun getSplitEmoji(title: String): String {
    val clean = title.lowercase(Locale.getDefault())
    return when {
        clean.contains("cena") || clean.contains("cibo") || clean.contains("ristorante") || clean.contains("mangiare") || clean.contains("sushi") || clean.contains("pizza") || clean.contains("pranzo") -> "🍕"
        clean.contains("regalo") || clean.contains("compleanno") || clean.contains("festa") -> "🎁"
        clean.contains("viaggio") || clean.contains("vacanza") || clean.contains("volo") || clean.contains("hotel") || clean.contains("albergo") -> "✈️"
        clean.contains("affitto") || clean.contains("casa") || clean.contains("bollette") || clean.contains("luce") || clean.contains("gas") || clean.contains("condominio") -> "🏠"
        clean.contains("taxi") || clean.contains("treno") || clean.contains("auto") || clean.contains("benzina") || clean.contains("diesel") || clean.contains("viaggio") -> "🚗"
        clean.contains("cinema") || clean.contains("film") || clean.contains("concerto") || clean.contains("show") || clean.contains("teatro") || clean.contains("netflix") -> "🎬"
        clean.contains("birra") || clean.contains("bar") || clean.contains("pub") || clean.contains("aperitivo") || clean.contains("drink") -> "🍺"
        clean.contains("spesa") || clean.contains("supermercato") || clean.contains("conad") || clean.contains("coop") || clean.contains("esselunga") -> "🛒"
        else -> "👥"
    }
}

private fun getSplitGroupHeader(dateMillis: Long): String {
    val today = Calendar.getInstance()
    val txDate = Calendar.getInstance().apply { timeInMillis = dateMillis }

    val isSameDay = today.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == txDate.get(Calendar.DAY_OF_YEAR)

    if (isSameDay) return "OGGI"

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == txDate.get(Calendar.DAY_OF_YEAR)

    if (isYesterday) return "IERI"

    // Days within last week
    val diffMillis = today.timeInMillis - txDate.timeInMillis
    val diffDays = diffMillis / (24 * 60 * 60 * 1000)
    if (diffDays in 2..6) {
        val sdfDayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdfDayOfWeek.format(Date(dateMillis)).uppercase(Locale.getDefault())
    }

    val sdfHeader = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return sdfHeader.format(Date(dateMillis)).uppercase(Locale.getDefault())
}

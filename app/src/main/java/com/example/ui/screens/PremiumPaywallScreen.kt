package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.premium.PremiumFeature
import com.example.premium.PremiumManager
import com.example.premium.PremiumPlan
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPaywallScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentPlanState by PremiumManager.currentPlan.collectAsState()
    val expirationDateState by PremiumManager.expirationDate.collectAsState()

    // Plan selection state for UI
    var selectedPlanOption by remember { mutableStateOf("monthly") } // "monthly" or "lifetime"

    Scaffold(
        containerColor = BackgroundWarm,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CashMate Premium",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("paywall_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Indietro",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWarm)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // HERO HEADER WITH GRADIENT
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                EmeraldPrimary.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(EmeraldPrimary, LimeSecondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Sblocca Tutto Con Premium",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Gestione intelligente dei tuoi soldi senza limiti, alimentata da intelligenza artificiale avanzata.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    if (PremiumManager.isPremiumActive()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100))
                                .background(EmeraldPrimary.copy(alpha = 0.15f))
                                .border(1.dp, EmeraldPrimary, RoundedCornerShape(100))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "PIANO ATTIVO: ${currentPlanState.name} ($expirationDateState)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }

            // VALUE PLAN CARDS / SELECTOR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SELEZIONA IL TUO PIANO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Monthly Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPlanOption == "monthly") SurfaceElevation else SurfaceWhite
                        ),
                        border = BorderStroke(
                            width = if (selectedPlanOption == "monthly") 2.dp else 1.dp,
                            color = if (selectedPlanOption == "monthly") EmeraldPrimary else Color.White.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedPlanOption = "monthly" }
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "PREMIUM PRO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selectedPlanOption == "monthly") EmeraldPrimary else TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "4.99 €",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "al mese",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Prova gratis 7 giorni",
                                fontSize = 10.sp,
                                color = LimeSecondary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Lifetime Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPlanOption == "lifetime") SurfaceElevation else SurfaceWhite
                        ),
                        border = BorderStroke(
                            width = if (selectedPlanOption == "lifetime") 2.dp else 1.dp,
                            color = if (selectedPlanOption == "lifetime") EmeraldPrimary else Color.White.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedPlanOption = "lifetime" }
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100))
                                    .background(LimeSecondary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "MIGLIOR VALORE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LimeSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "LIFETIME",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selectedPlanOption == "lifetime") EmeraldPrimary else TextSecondary
                            )
                            Text(
                                text = "29.99 €",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "una tantum per sempre",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // PRIMARY CTAS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        Toast.makeText(context, "Acquisti non configurati in questa versione.", Toast.LENGTH_LONG).show()
                    },
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("paywall_cta_button")
                ) {
                    Text(
                        text = "Acquisti non disponibili",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            Toast.makeText(context, "Ripristino acquisti non configurato.", Toast.LENGTH_LONG).show()
                        },
                        enabled = false,
                        modifier = Modifier.testTag("restore_purchases_button")
                    ) {
                        Text(
                            text = "Ripristina acquisti",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textDecoration = TextDecoration.Underline
                        )
                    }

                    Text(
                        text = "Cancella quando vuoi",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // VALUE COMPARISON CHART (Free vs Premium)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "CONFRONTO FUNZIONALITÀ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Table Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Funzione",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.weight(1.5f)
                            )
                            Text(
                                text = "Free",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Premium",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.04f))

                        // Features List comparison
                        val compList = listOf(
                            Triple("Richieste AI Coach", PremiumManager.getLimitDescription(PremiumFeature.AI_CHAT), "Senza limiti (30/ora)"),
                            Triple("Scansione Scontrini OCR", PremiumManager.getLimitDescription(PremiumFeature.OCR), "Illimitati"),
                            Triple("Album Split Bill", PremiumManager.getLimitDescription(PremiumFeature.UNLIMITED_SPLITS), "Illimitati"),
                            Triple("Conti e Wallet", PremiumManager.getLimitDescription(PremiumFeature.MULTI_ACCOUNT), "Illimitati"),
                            Triple("Esportazione CSV/JSON", "Disabilitato", "Attivo"),
                            Triple("Sincronizzazione Cloud", "Locale", "Attivo"),
                            Triple("Widget Schermata Home", "Disabilitato", "Attivo"),
                            Triple("Regole Transazioni", "Limitate", "Avanzate"),
                            Triple("Obiettivi Risparmio", PremiumManager.getLimitDescription(PremiumFeature.GOALS), "Illimitati")
                        )

                        compList.forEach { (featName, freeVal, premiumVal) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = featName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    modifier = Modifier.weight(1.5f)
                                )
                                Text(
                                    text = freeVal,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = premiumVal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.03f))
                        }
                    }
                }
            }

            // DETAILED BENEFITS WITH ELEGANT CARDS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "TUTTI I BENEFICI PRO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                PremiumFeature.values().forEach { feature ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when (feature) {
                                    PremiumFeature.AI_CHAT -> Icons.Default.AutoAwesome
                                    PremiumFeature.OCR -> Icons.Default.QrCodeScanner
                                    PremiumFeature.UNLIMITED_SPLITS -> Icons.Default.Group
                                    PremiumFeature.MULTI_ACCOUNT -> Icons.Default.AccountBalanceWallet
                                    PremiumFeature.EXPORT -> Icons.Default.Download
                                    PremiumFeature.CLOUD_SYNC -> Icons.Default.CloudSync
                                    PremiumFeature.WIDGET -> Icons.Default.Widgets
                                    PremiumFeature.SMART_RULES -> Icons.Default.Settings
                                    PremiumFeature.GOALS -> Icons.Default.TrendingUp
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = feature.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = feature.description,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

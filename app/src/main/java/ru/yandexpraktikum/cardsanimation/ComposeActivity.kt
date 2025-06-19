package ru.yandexpraktikum.cardsanimation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.yandexpraktikum.cardsanimation.compose.AnimatedCardStack
import ru.yandexpraktikum.cardsanimation.model.CardData
import ru.yandexpraktikum.cardsanimation.ui.theme.CardsAnimationTheme

class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CardsAnimationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AnimatedCardScreen(
                        modifier = Modifier.padding(innerPadding),
                        cards = listOf(
                            CardData(R.drawable.card_clover),
                            CardData(R.drawable.card_hearts),
                            CardData(R.drawable.card_spades),
                            CardData(R.drawable.card_diamond)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedCardScreen(
    modifier: Modifier = Modifier,
    cards: List<CardData>
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.jetpack_compose_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        AnimatedCardStack(cards = cards)

        Spacer(modifier = Modifier.height(60.dp))

        Button(
            onClick = {
                context.startActivity(Intent(context, XmlViewActivity::class.java))
            }
        ) {
            Text("Просмотреть версию на XML View")
        }
    }
}
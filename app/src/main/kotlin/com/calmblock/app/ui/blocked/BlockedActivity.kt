package com.calmblock.app.ui.blocked

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calmblock.app.core.Quotes
import com.calmblock.app.data.AppDb
import com.calmblock.app.data.BlockedEventEntity
import com.calmblock.app.data.Prefs
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.calmblock.app.ui.Ink
import com.calmblock.app.ui.OutlineButton
import com.calmblock.app.ui.theme.Type

/** The screen that stands in for a blocked app or website. One way out: home. */
class BlockedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "This app"
        val prefs = Prefs(this)
        val index = Quotes.pick(prefs.lastQuote)
        prefs.lastQuote = index
        val quote = Quotes.all[index]
        val kind = intent.getStringExtra(EXTRA_KIND) ?: KIND_APP
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { AppDb.get(applicationContext).dao().insertEvent(BlockedEventEntity(date = LocalDate.now().toString(), label = label, kind = kind, at = System.currentTimeMillis())) }
        }

        setContent {
            Column(
                Modifier.fillMaxSize().background(Ink.bg).systemBarsPadding().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(label, style = Type.hero)
                Spacer(Modifier.height(10.dp))
                Text("Blocked", style = Type.row.copy(color = Ink.textSecondary))
                Spacer(Modifier.height(48.dp))
                Text("“${quote.text}”", style = Type.quote)
                Spacer(Modifier.height(16.dp))
                Text(quote.author, style = Type.meta)
                Spacer(Modifier.height(56.dp))
                OutlineButton("Back", Modifier.fillMaxWidth()) { goHome() }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = goHome()

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_LABEL = "label"
        const val KIND_APP = "app"
        const val KIND_SITE = "site"
    }
}

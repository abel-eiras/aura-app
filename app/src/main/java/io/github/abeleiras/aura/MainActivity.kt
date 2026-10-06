package io.github.abeleiras.aura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.abeleiras.aura.ui.theme.AuraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        setContent {
            AuraTheme {
                PlaceholderScreen(versionName)
            }
        }
    }
}

/** Temporary screen for v0.1.0: proves install + update flow only (ROADMAP, hito 0). */
@Composable
private fun PlaceholderScreen(versionName: String) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.placeholder_title), style = MaterialTheme.typography.displayMedium)
            Text(stringResource(R.string.placeholder_body), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.placeholder_version, versionName), style = MaterialTheme.typography.labelMedium)
        }
    }
}

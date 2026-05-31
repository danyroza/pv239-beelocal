package com.pv239.beelocal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pv239.beelocal.navigation.AppNavGraph
import com.pv239.beelocal.ui.theme.BeelocalTheme
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = "BeeLocal"
        enableEdgeToEdge()
        setContent {
            BeelocalTheme {
                AppNavGraph()
            }
        }
    }

}

package com.git.log.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.git.log.common.components.TypewriterText
import com.git.log.common.tool.AppColors
import com.git.log.feature.home.viewmodel.HomeViewModel

private val helloWorld = listOf(
    """print("Hello World")""", // Python
    """System.out.println("Hello World");""", // Java
    """printf("Hello World");""", // C
    """std::cout << "Hello World" << std::endl;""", // C++
    """console.log("Hello World");""", // JavaScript
    """puts "Hello World" """, // Ruby
    """<?php echo "Hello World"; ?>""", // PHP
    """print("Hello World")""", // Swift
    """fmt.Println("Hello World")""", // Go
    """Console.WriteLine("Hello World");""", // C#
    """echo "Hello World";""", // Bash
    """Write-Output "Hello World"""", // PowerShell
    """echo('Hello World')""", // Lua
    """(println "Hello World")""", // Clojure
    """echo Hello, World!""", // Batch
    """DISPLAY 'Hello World'""", // COBOL
    """write('Hello World')""", // Pascal
    """io.write("Hello World")""", // Lua
    """print *, "Hello World"""", // Fortran
    """PRINT "Hello World" """, // BASIC
    """printf("Hello World");""", // Kotlin (via C interop)
    """System.Console.WriteLine("Hello World");""", // F#
    """print_endline "Hello World";""", // OCaml
    """IO.puts("Hello World")""", // Elixir
    """print("Hello World!")""", // R
)

@Composable
fun Home() {
    HomePage()
}

@Composable
fun HomePage() {
    Column (
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // title
        HomeTitle()
        // content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 60.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            HomeContent()
        }
        // tail
        HomeFooter()
    }
}

@Composable
fun HomeTitle() {
    Text(
        text = "Git Log For Year",
        style = MaterialTheme.typography.headlineMedium,
        color = AppColors.TextPrimary,
        modifier = Modifier
            .padding(top = 20.dp)
    )
}

@Composable
fun HomeContent() {
    val viewModel = remember { HomeViewModel() }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        TypewriterText(
            helloWorld,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .height(35.dp)
        )
        MainActionButtons(
            onClick = {
                print("测试")
            },
            contentColor = Color(0xFF637172).copy(alpha = 0.2f)

        )
        DataSourceDesc(
            modifier = Modifier
                .padding(top = 20.dp),
            onClickItem = { viewModel.onDataSourceClicked(it) }
        )
    }
}

@Composable
fun HomeFooter() {
    Column (
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val uriHandler = LocalUriHandler.current
        Row(
            modifier = Modifier
                .padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Made by ",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = AppColors.TextSecondary,
            )

            Text(
                text = "@AkaShark",
                style = MaterialTheme.typography.bodySmall.copy(
                    textDecoration = TextDecoration.Underline,
                    fontSize = 10.sp
                ),
                color = AppColors.TextSecondary,
                modifier = Modifier
                    .clickable(role = Role.Button) {
                        uriHandler.openUri("https://github.com/AkaShark")
                    }
            )
        }
        Row(
            modifier = Modifier
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Thanks to ",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = AppColors.TextSecondary
            )
            Text(
                text = "@Lakr233",
                style = MaterialTheme.typography.bodySmall.copy(
                    textDecoration = TextDecoration.Underline,
                    fontSize = 10.sp
                ),
                color = AppColors.TextSecondary,
                modifier = Modifier
                    .clickable(role = Role.Button) {
                        uriHandler.openUri("https://github.com/Lakr233")
                    }
            )
        }

    }
}

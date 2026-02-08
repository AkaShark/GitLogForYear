package com.git.log.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.git.log.common.components.TypewriterText
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

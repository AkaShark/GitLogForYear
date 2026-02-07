package com.git.log.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.git.log.common.components.TypewriterText

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
fun Home(
    onOpenReport: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        TypewriterText(helloWorld)
    }
}
package com.goprex.ui.produto

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object CategoriasProduto {
    const val OUTROS = "Outros"

    val fixas = listOf(
        "Eletrônicos e celulares",
        "Eletrodomésticos",
        "Casa, móveis e decoração",
        "Ferramentas e construção",
        "Moda e acessórios",
        "Esportes e lazer",
        "Veículos e peças",
        "Bebês e crianças",
        "Saúde e beleza",
        "Agro e indústria",
        "Alimentos e bebidas"
    )

    val opcoes = fixas + OUTROS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriaProdutoField(
    categoriaAtual: String,
    onCategoriaChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    val categoriaFixaInicial = categoriaAtual.takeIf { it in CategoriasProduto.fixas }
    var categoriaSelecionada by rememberSaveable {
        mutableStateOf(
            when {
                categoriaFixaInicial != null -> categoriaFixaInicial
                categoriaAtual.isNotBlank() -> CategoriasProduto.OUTROS
                else -> ""
            }
        )
    }
    var categoriaPersonalizada by rememberSaveable {
        mutableStateOf(
            categoriaAtual.takeIf {
                it.isNotBlank() && it !in CategoriasProduto.fixas
            }.orEmpty()
        )
    }
    var menuExpandido by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = menuExpandido,
            onExpandedChange = { menuExpandido = !menuExpandido }
        ) {
            OutlinedTextField(
                value = categoriaSelecionada,
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoria *") },
                placeholder = { Text("Selecione") },
                leadingIcon = {
                    androidx.compose.material3.Icon(Icons.Default.Category, null)
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpandido)
                },
                isError = isError,
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = menuExpandido,
                onDismissRequest = { menuExpandido = false }
            ) {
                CategoriasProduto.opcoes.forEach { categoria ->
                    DropdownMenuItem(
                        text = { Text(categoria) },
                        onClick = {
                            categoriaSelecionada = categoria
                            menuExpandido = false
                            if (categoria == CategoriasProduto.OUTROS) {
                                onCategoriaChange(categoriaPersonalizada.trim())
                            } else {
                                categoriaPersonalizada = ""
                                onCategoriaChange(categoria)
                            }
                        }
                    )
                }
            }
        }

        if (categoriaSelecionada == CategoriasProduto.OUTROS) {
            OutlinedTextField(
                value = categoriaPersonalizada,
                onValueChange = {
                    categoriaPersonalizada = it
                    onCategoriaChange(it)
                },
                label = { Text("Informe a categoria *") },
                singleLine = true,
                isError = isError && categoriaPersonalizada.isBlank(),
                supportingText = if (isError && categoriaPersonalizada.isBlank()) {
                    { Text("Informe uma categoria") }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

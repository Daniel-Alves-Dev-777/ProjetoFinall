package com.example.projetofinal

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Context.dataStore by preferencesDataStore(name = "configuracoes_pdm")

private val formatoData: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val formatoMoeda: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["usuario"], unique = true)]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val usuario: String,
    val senha: String
)

@Entity(tableName = "clientes")
data class ClienteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nome: String,
    val telefone: String,
    val email: String,
    val cidade: String
)

@Entity(tableName = "produtos")
data class ProdutoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nome: String,
    val descricao: String,
    val valor: Double,
    val estoque: Int
)

@Entity(
    tableName = "pedidos",
    foreignKeys = [
        ForeignKey(
            entity = ClienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["clienteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProdutoEntity::class,
            parentColumns = ["id"],
            childColumns = ["produtoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("clienteId"),
        Index("produtoId")
    ]
)
data class PedidoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val clienteId: Int,
    val produtoId: Int,
    val quantidade: Int,
    val dataPedido: String,
    val horaPedido: String,
    val valorTotal: Double
)

data class PedidoComDetalhes(
    @Embedded
    val pedido: PedidoEntity,

    @Relation(
        parentColumn = "clienteId",
        entityColumn = "id"
    )
    val cliente: ClienteEntity,

    @Relation(
        parentColumn = "produtoId",
        entityColumn = "id"
    )
    val produto: ProdutoEntity
)

@Dao
interface AppDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserirUsuario(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios WHERE usuario = :usuario AND senha = :senha LIMIT 1")
    suspend fun validarLogin(usuario: String, senha: String): UsuarioEntity?

    @Insert
    suspend fun inserirCliente(cliente: ClienteEntity)

    @Update
    suspend fun atualizarCliente(cliente: ClienteEntity)

    @Delete
    suspend fun excluirCliente(cliente: ClienteEntity)

    @Query("SELECT * FROM clientes ORDER BY nome ASC")
    fun listarClientes(): Flow<List<ClienteEntity>>

    @Query("SELECT COUNT(*) FROM clientes")
    fun contarClientes(): Flow<Int>

    @Insert
    suspend fun inserirProduto(produto: ProdutoEntity)

    @Update
    suspend fun atualizarProduto(produto: ProdutoEntity)

    @Delete
    suspend fun excluirProduto(produto: ProdutoEntity)

    @Query("SELECT * FROM produtos ORDER BY nome ASC")
    fun listarProdutos(): Flow<List<ProdutoEntity>>

    @Query("SELECT COUNT(*) FROM produtos")
    fun contarProdutos(): Flow<Int>

    @Insert
    suspend fun inserirPedido(pedido: PedidoEntity)

    @Update
    suspend fun atualizarPedido(pedido: PedidoEntity)

    @Delete
    suspend fun excluirPedido(pedido: PedidoEntity)

    @Transaction
    @Query("SELECT * FROM pedidos ORDER BY id DESC")
    fun listarPedidosComDetalhes(): Flow<List<PedidoComDetalhes>>

    @Query("SELECT COUNT(*) FROM pedidos")
    fun contarPedidos(): Flow<Int>
}

@Database(
    entities = [
        UsuarioEntity::class,
        ClienteEntity::class,
        ProdutoEntity::class,
        PedidoEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "projeto_final_pdm.db"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}

data class ConfigUiState(
    val temaEscuro: Boolean = false,
    val nomeUsuario: String = "Administrador",
    val notificacoesAtivadas: Boolean = true,
    val ordenacao: String = "Mais recente"
)

object ConfigKeys {
    val TEMA_ESCURO = booleanPreferencesKey("tema_escuro")
    val NOME_USUARIO = stringPreferencesKey("nome_usuario")
    val NOTIFICACOES = booleanPreferencesKey("notificacoes")
    val ORDENACAO = stringPreferencesKey("ordenacao")
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val dao = AppDatabase.getDatabase(application).appDao()

    val configuracoes = context.dataStore.data.map { prefs ->
        ConfigUiState(
            temaEscuro = prefs[ConfigKeys.TEMA_ESCURO] ?: false,
            nomeUsuario = prefs[ConfigKeys.NOME_USUARIO] ?: "Administrador",
            notificacoesAtivadas = prefs[ConfigKeys.NOTIFICACOES] ?: true,
            ordenacao = prefs[ConfigKeys.ORDENACAO] ?: "Mais recente"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConfigUiState()
    )

    val clientes = dao.listarClientes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val produtos = dao.listarProdutos().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val pedidos = combine(
        dao.listarPedidosComDetalhes(),
        configuracoes
    ) { lista, config ->
        when (config.ordenacao) {
            "Cliente" -> lista.sortedBy { it.cliente.nome.lowercase() }
            "Produto" -> lista.sortedBy { it.produto.nome.lowercase() }
            "Valor" -> lista.sortedByDescending { it.pedido.valorTotal }
            else -> lista.sortedByDescending { it.pedido.id }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val qtdClientes = dao.contarClientes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val qtdProdutos = dao.contarProdutos().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val qtdPedidos = dao.contarPedidos().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    init {
        viewModelScope.launch {
            criarUsuarioPadrao()
        }
    }

    private suspend fun criarUsuarioPadrao() {
        dao.inserirUsuario(
            UsuarioEntity(
                usuario = "admin",
                senha = "1234"
            )
        )
    }

    fun fazerLogin(
        usuario: String,
        senha: String,
        resultado: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            criarUsuarioPadrao()

            val usuarioEncontrado = dao.validarLogin(
                usuario = usuario.trim(),
                senha = senha.trim()
            )

            resultado(usuarioEncontrado != null)
        }
    }

    fun salvarCliente(
        id: Int?,
        nome: String,
        telefone: String,
        email: String,
        cidade: String
    ) {
        viewModelScope.launch {
            val cliente = ClienteEntity(
                id = id ?: 0,
                nome = nome.trim(),
                telefone = telefone.trim(),
                email = email.trim(),
                cidade = cidade.trim()
            )

            if (id == null) {
                dao.inserirCliente(cliente)
            } else {
                dao.atualizarCliente(cliente)
            }
        }
    }

    fun excluirCliente(cliente: ClienteEntity) {
        viewModelScope.launch {
            dao.excluirCliente(cliente)
        }
    }

    fun salvarProduto(
        id: Int?,
        nome: String,
        descricao: String,
        valor: Double,
        estoque: Int
    ) {
        viewModelScope.launch {
            val produto = ProdutoEntity(
                id = id ?: 0,
                nome = nome.trim(),
                descricao = descricao.trim(),
                valor = valor,
                estoque = estoque
            )

            if (id == null) {
                dao.inserirProduto(produto)
            } else {
                dao.atualizarProduto(produto)
            }
        }
    }

    fun excluirProduto(produto: ProdutoEntity) {
        viewModelScope.launch {
            dao.excluirProduto(produto)
        }
    }

    fun salvarPedido(
        id: Int?,
        clienteId: Int,
        produtoId: Int,
        quantidade: Int,
        dataPedido: String,
        horaPedido: String,
        valorTotal: Double
    ) {
        viewModelScope.launch {
            val pedido = PedidoEntity(
                id = id ?: 0,
                clienteId = clienteId,
                produtoId = produtoId,
                quantidade = quantidade,
                dataPedido = dataPedido,
                horaPedido = horaPedido,
                valorTotal = valorTotal
            )

            if (id == null) {
                dao.inserirPedido(pedido)
            } else {
                dao.atualizarPedido(pedido)
            }
        }
    }

    fun excluirPedido(pedido: PedidoEntity) {
        viewModelScope.launch {
            dao.excluirPedido(pedido)
        }
    }

    fun salvarTemaEscuro(valor: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[ConfigKeys.TEMA_ESCURO] = valor
            }
        }
    }

    fun salvarNomeUsuario(nome: String) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[ConfigKeys.NOME_USUARIO] = nome.trim().ifBlank { "Administrador" }
            }
        }
    }

    fun salvarNotificacoes(valor: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[ConfigKeys.NOTIFICACOES] = valor
            }
        }
    }

    fun salvarOrdenacao(valor: String) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[ConfigKeys.ORDENACAO] = valor
            }
        }
    }
}

class AppViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppViewModel(application) as T
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: AppViewModel = viewModel(
                factory = AppViewModelFactory(application)
            )

            val config by viewModel.configuracoes.collectAsState()

            ProjetoFinalTheme(temaEscuro = config.temaEscuro) {
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ProjetoFinalTheme(
    temaEscuro: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (temaEscuro) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}

@Composable
fun AppNavigation(viewModel: AppViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            TelaLogin(
                viewModel = viewModel,
                aoEntrar = {
                    navController.navigate("principal") {
                        popUpTo("login") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable("principal") {
            TelaPrincipal(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable("clientes") {
            TelaClientes(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable("produtos") {
            TelaProdutos(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable("pedidos") {
            TelaPedidos(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable("configuracoes") {
            TelaConfiguracoes(
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun TelaLogin(
    viewModel: AppViewModel,
    aoEntrar: () -> Unit
) {
    var usuario by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Controle de Pedidos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Login do sistema",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = usuario,
                onValueChange = { usuario = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Usuário") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Senha") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (usuario.isBlank() || senha.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Preencha usuário e senha.")
                        }
                        return@Button
                    }

                    viewModel.fazerLogin(usuario, senha) { ok ->
                        if (ok) {
                            aoEntrar()
                        } else {
                            Toast.makeText(
                                context,
                                "Usuário ou senha incorretos",
                                Toast.LENGTH_SHORT
                            ).show()

                            scope.launch {
                                snackbarHostState.showSnackbar("Usuário ou senha incorretos.")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Entrar")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Usuário padrão: admin | Senha: 1234",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaBase(
    titulo: String,
    navController: NavController?,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(titulo)
                },
                navigationIcon = {
                    if (navController != null) {
                        TextButton(
                            onClick = {
                                navController.popBackStack()
                            }
                        ) {
                            Text("Voltar")
                        }
                    }
                }
            )
        },
        content = content
    )
}

@Composable
fun TelaPrincipal(
    navController: NavController,
    viewModel: AppViewModel
) {
    val qtdClientes by viewModel.qtdClientes.collectAsState()
    val qtdProdutos by viewModel.qtdProdutos.collectAsState()
    val qtdPedidos by viewModel.qtdPedidos.collectAsState()
    val config by viewModel.configuracoes.collectAsState()

    TelaBase(
        titulo = "Tela Principal",
        navController = null
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Olá, ${config.nomeUsuario}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Resumo do sistema",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                CardResumo(
                    titulo = "Clientes cadastrados",
                    valor = qtdClientes.toString()
                )
            }

            item {
                CardResumo(
                    titulo = "Produtos cadastrados",
                    valor = qtdProdutos.toString()
                )
            }

            item {
                CardResumo(
                    titulo = "Pedidos cadastrados",
                    valor = qtdPedidos.toString()
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Button(
                    onClick = { navController.navigate("clientes") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clientes")
                }
            }

            item {
                Button(
                    onClick = { navController.navigate("produtos") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Produtos")
                }
            }

            item {
                Button(
                    onClick = { navController.navigate("pedidos") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pedidos")
                }
            }

            item {
                OutlinedButton(
                    onClick = { navController.navigate("configuracoes") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Configurações")
                }
            }
        }
    }
}

@Composable
fun CardResumo(
    titulo: String,
    valor: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = valor,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TelaClientes(
    navController: NavController,
    viewModel: AppViewModel
) {
    val clientes by viewModel.clientes.collectAsState()
    val context = LocalContext.current

    var clienteEditando by remember { mutableStateOf<ClienteEntity?>(null) }
    var nome by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }

    fun limparFormulario() {
        clienteEditando = null
        nome = ""
        telefone = ""
        email = ""
        cidade = ""
    }

    TelaBase(
        titulo = "Clientes",
        navController = navController
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = if (clienteEditando == null) "Cadastrar cliente" else "Editar cliente",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = telefone,
                    onValueChange = { telefone = it },
                    label = { Text("Telefone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = cidade,
                    onValueChange = { cidade = it },
                    label = { Text("Cidade") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (nome.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Informe o nome do cliente.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            viewModel.salvarCliente(
                                id = clienteEditando?.id,
                                nome = nome,
                                telefone = telefone,
                                email = email,
                                cidade = cidade
                            )

                            limparFormulario()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (clienteEditando == null) "Inserir" else "Salvar")
                    }

                    OutlinedButton(
                        onClick = { limparFormulario() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Limpar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Lista de clientes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (clientes.isEmpty()) {
                item {
                    Text("Nenhum cliente cadastrado.")
                }
            } else {
                items(clientes, key = { it.id }) { cliente ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = cliente.nome,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text("Telefone: ${cliente.telefone}")
                            Text("E-mail: ${cliente.email}")
                            Text("Cidade: ${cliente.cidade}")

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clienteEditando = cliente
                                        nome = cliente.nome
                                        telefone = cliente.telefone
                                        email = cliente.email
                                        cidade = cliente.cidade
                                    }
                                ) {
                                    Text("Editar")
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.excluirCliente(cliente)
                                    }
                                ) {
                                    Text("Excluir")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelaProdutos(
    navController: NavController,
    viewModel: AppViewModel
) {
    val produtos by viewModel.produtos.collectAsState()
    val context = LocalContext.current

    var produtoEditando by remember { mutableStateOf<ProdutoEntity?>(null) }
    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var valor by remember { mutableStateOf("") }
    var estoque by remember { mutableStateOf("") }

    fun limparFormulario() {
        produtoEditando = null
        nome = ""
        descricao = ""
        valor = ""
        estoque = ""
    }

    TelaBase(
        titulo = "Produtos",
        navController = navController
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = if (produtoEditando == null) "Cadastrar produto" else "Editar produto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do produto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = it },
                    label = { Text("Valor") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = estoque,
                    onValueChange = { estoque = it },
                    label = { Text("Quantidade em estoque") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val valorDouble = valor.replace(",", ".").toDoubleOrNull()
                            val estoqueInt = estoque.toIntOrNull()

                            if (nome.isBlank() || valorDouble == null || estoqueInt == null) {
                                Toast.makeText(
                                    context,
                                    "Preencha nome, valor e estoque corretamente.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            viewModel.salvarProduto(
                                id = produtoEditando?.id,
                                nome = nome,
                                descricao = descricao,
                                valor = valorDouble,
                                estoque = estoqueInt
                            )

                            limparFormulario()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (produtoEditando == null) "Inserir" else "Salvar")
                    }

                    OutlinedButton(
                        onClick = { limparFormulario() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Limpar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Lista de produtos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (produtos.isEmpty()) {
                item {
                    Text("Nenhum produto cadastrado.")
                }
            } else {
                items(produtos, key = { it.id }) { produto ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = produto.nome,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text("Descrição: ${produto.descricao}")
                            Text("Valor: ${formatarMoeda(produto.valor)}")
                            Text("Estoque: ${produto.estoque}")

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        produtoEditando = produto
                                        nome = produto.nome
                                        descricao = produto.descricao
                                        valor = produto.valor.toString()
                                        estoque = produto.estoque.toString()
                                    }
                                ) {
                                    Text("Editar")
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.excluirProduto(produto)
                                    }
                                ) {
                                    Text("Excluir")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPedidos(
    navController: NavController,
    viewModel: AppViewModel
) {
    val clientes by viewModel.clientes.collectAsState()
    val produtos by viewModel.produtos.collectAsState()
    val pedidos by viewModel.pedidos.collectAsState()
    val context = LocalContext.current

    var pedidoEditando by remember { mutableStateOf<PedidoEntity?>(null) }
    var clienteIdSelecionado by remember { mutableStateOf<Int?>(null) }
    var produtoIdSelecionado by remember { mutableStateOf<Int?>(null) }
    var quantidade by remember { mutableStateOf("") }
    var dataPedido by remember {
        mutableStateOf(LocalDate.now().format(formatoData))
    }
    var horaPedido by remember {
        val agora = LocalTime.now()
        mutableStateOf("%02d:%02d".format(agora.hour, agora.minute))
    }

    var mostrarDatePicker by remember { mutableStateOf(false) }
    var mostrarTimePicker by remember { mutableStateOf(false) }
    var pedidoParaExcluir by remember { mutableStateOf<PedidoEntity?>(null) }

    val produtoSelecionado = produtos.firstOrNull { it.id == produtoIdSelecionado }
    val quantidadeInt = quantidade.toIntOrNull() ?: 0
    val valorTotal = (produtoSelecionado?.valor ?: 0.0) * quantidadeInt

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(
        initialHour = LocalTime.now().hour,
        initialMinute = LocalTime.now().minute,
        is24Hour = true
    )

    fun limparFormulario() {
        pedidoEditando = null
        clienteIdSelecionado = null
        produtoIdSelecionado = null
        quantidade = ""
        dataPedido = LocalDate.now().format(formatoData)

        val agora = LocalTime.now()
        horaPedido = "%02d:%02d".format(agora.hour, agora.minute)
    }

    if (mostrarDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                mostrarDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis

                        if (millis != null) {
                            dataPedido = Instant
                                .ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .format(formatoData)
                        }

                        mostrarDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarDatePicker = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (mostrarTimePicker) {
        AlertDialog(
            onDismissRequest = {
                mostrarTimePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        horaPedido = "%02d:%02d".format(
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        mostrarTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarTimePicker = false
                    }
                ) {
                    Text("Cancelar")
                }
            },
            title = {
                Text("Escolha a hora")
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    if (pedidoParaExcluir != null) {
        AlertDialog(
            onDismissRequest = {
                pedidoParaExcluir = null
            },
            title = {
                Text("Confirmar exclusão")
            },
            text = {
                Text("Deseja realmente excluir este pedido?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pedidoParaExcluir?.let {
                            viewModel.excluirPedido(it)
                        }
                        pedidoParaExcluir = null
                    }
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pedidoParaExcluir = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    TelaBase(
        titulo = "Pedidos",
        navController = navController
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = if (pedidoEditando == null) "Cadastrar pedido" else "Editar pedido",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                SelecionadorSimples(
                    label = "Cliente",
                    opcoes = clientes,
                    selecionado = clientes.firstOrNull { it.id == clienteIdSelecionado },
                    texto = { it.nome },
                    aoSelecionar = {
                        clienteIdSelecionado = it.id
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SelecionadorSimples(
                    label = "Produto",
                    opcoes = produtos,
                    selecionado = produtos.firstOrNull { it.id == produtoIdSelecionado },
                    texto = { "${it.nome} - ${formatarMoeda(it.valor)}" },
                    aoSelecionar = {
                        produtoIdSelecionado = it.id
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = quantidade,
                    onValueChange = { quantidade = it },
                    label = { Text("Quantidade") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            mostrarDatePicker = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Data: $dataPedido")
                    }

                    OutlinedButton(
                        onClick = {
                            mostrarTimePicker = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hora: $horaPedido")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Valor total: ${formatarMoeda(valorTotal)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val clienteId = clienteIdSelecionado
                            val produtoId = produtoIdSelecionado

                            if (clienteId == null || produtoId == null) {
                                Toast.makeText(
                                    context,
                                    "Selecione cliente e produto.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            if (quantidadeInt <= 0) {
                                Toast.makeText(
                                    context,
                                    "Informe uma quantidade válida.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            viewModel.salvarPedido(
                                id = pedidoEditando?.id,
                                clienteId = clienteId,
                                produtoId = produtoId,
                                quantidade = quantidadeInt,
                                dataPedido = dataPedido,
                                horaPedido = horaPedido,
                                valorTotal = valorTotal
                            )

                            limparFormulario()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (pedidoEditando == null) "Inserir" else "Salvar")
                    }

                    OutlinedButton(
                        onClick = {
                            limparFormulario()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Limpar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Lista de pedidos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (pedidos.isEmpty()) {
                item {
                    Text("Nenhum pedido cadastrado.")
                }
            } else {
                items(pedidos, key = { it.pedido.id }) { detalhe ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = "Pedido #${detalhe.pedido.id}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text("Cliente: ${detalhe.cliente.nome}")
                            Text("Produto: ${detalhe.produto.nome}")
                            Text("Quantidade: ${detalhe.pedido.quantidade}")
                            Text("Data: ${detalhe.pedido.dataPedido}")
                            Text("Hora: ${detalhe.pedido.horaPedido}")
                            Text("Total: ${formatarMoeda(detalhe.pedido.valorTotal)}")

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        pedidoEditando = detalhe.pedido
                                        clienteIdSelecionado = detalhe.pedido.clienteId
                                        produtoIdSelecionado = detalhe.pedido.produtoId
                                        quantidade = detalhe.pedido.quantidade.toString()
                                        dataPedido = detalhe.pedido.dataPedido
                                        horaPedido = detalhe.pedido.horaPedido
                                    }
                                ) {
                                    Text("Editar")
                                }

                                OutlinedButton(
                                    onClick = {
                                        pedidoParaExcluir = detalhe.pedido
                                    }
                                ) {
                                    Text("Excluir")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelaConfiguracoes(
    navController: NavController,
    viewModel: AppViewModel
) {
    val config by viewModel.configuracoes.collectAsState()
    val context = LocalContext.current

    var nomeUsuario by remember(config.nomeUsuario) {
        mutableStateOf(config.nomeUsuario)
    }

    TelaBase(
        titulo = "Configurações",
        navController = navController
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "Preferências do sistema",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Tema escuro",
                                fontWeight = FontWeight.Bold
                            )
                            Text("Ativar ou desativar modo escuro")
                        }

                        Switch(
                            checked = config.temaEscuro,
                            onCheckedChange = {
                                viewModel.salvarTemaEscuro(it)
                            }
                        )
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Notificações",
                                fontWeight = FontWeight.Bold
                            )
                            Text("Preferência salva localmente")
                        }

                        Switch(
                            checked = config.notificacoesAtivadas,
                            onCheckedChange = {
                                viewModel.salvarNotificacoes(it)
                            }
                        )
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "Nome do usuário",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = nomeUsuario,
                            onValueChange = { nomeUsuario = it },
                            label = { Text("Nome") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.salvarNomeUsuario(nomeUsuario)
                                Toast.makeText(
                                    context,
                                    "Nome salvo.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Salvar nome")
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "Preferência de ordenação dos pedidos",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SelecionadorSimples(
                            label = "Ordenar por",
                            opcoes = listOf("Mais recente", "Cliente", "Produto", "Valor"),
                            selecionado = config.ordenacao,
                            texto = { it },
                            aoSelecionar = {
                                viewModel.salvarOrdenacao(it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun <T> SelecionadorSimples(
    label: String,
    opcoes: List<T>,
    selecionado: T?,
    texto: (T) -> String,
    aoSelecionar: (T) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = {
                    expandido = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selecionado?.let { texto(it) } ?: "Selecione"
                )
            }

            DropdownMenu(
                expanded = expandido,
                onDismissRequest = {
                    expandido = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (opcoes.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text("Nenhum item cadastrado")
                        },
                        onClick = {
                            expandido = false
                        }
                    )
                } else {
                    opcoes.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Text(texto(item))
                            },
                            onClick = {
                                aoSelecionar(item)
                                expandido = false
                            }
                        )
                    }
                }
            }
        }
    }
}

fun formatarMoeda(valor: Double): String {
    return formatoMoeda.format(valor)
}
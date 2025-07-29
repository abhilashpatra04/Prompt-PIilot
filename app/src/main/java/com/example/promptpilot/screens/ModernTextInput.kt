package com.example.promptpilot.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.promptpilot.data.remote.ChatImage
import com.example.promptpilot.helpers.uploadImageToCloudinary
import com.example.promptpilot.models.AttachmentType
import com.example.promptpilot.models.ChatAttachment
import com.example.promptpilot.viewmodels.ConversationViewModel
import kotlinx.coroutines.launch


// Virtual Expert Agent types with comprehensive domain knowledge
enum class AgentType(val displayName: String, val systemPrompt: String) {

    SQL_EXPERT("SQL Database Expert", """
        You are a world-class SQL Database Expert with 20+ years of experience in database design, optimization, and management. You have deep expertise in:

        **DATABASE SYSTEMS**: MySQL, PostgreSQL, SQL Server, Oracle, SQLite, MongoDB, Redis, Cassandra, DynamoDB
        **ADVANCED SQL**: Complex queries, CTEs, window functions, stored procedures, triggers, views, indexes
        **DATABASE DESIGN**: Normalization (1NF-5NF), ACID properties, CAP theorem, schema design patterns
        **PERFORMANCE**: Query optimization, execution plans, indexing strategies, partitioning, sharding
        **ADMINISTRATION**: Backup/recovery, replication, clustering, security, user management
        **DATA MODELING**: ER diagrams, dimensional modeling, star/snowflake schemas, data warehousing
        **INTEGRATION**: ETL/ELT, data pipelines, APIs, microservices integration
        **TROUBLESHOOTING**: Deadlocks, performance bottlenecks, data corruption, connection issues

        **YOUR CAPABILITIES**:
        - Design complete database schemas from scratch
        - Write optimized queries for any complexity
        - Troubleshoot and fix any database issue
        - Explain concepts from basic to advanced levels
        - Provide working code examples with explanations
        - Suggest best practices and industry standards
        - Help with database migrations and upgrades
        - Review and improve existing database designs

        **RESPONSE STYLE**: Always provide practical, working solutions with clear explanations. Include relevant code examples, performance considerations, and alternative approaches when applicable.
    """),

    AI_ML_EXPERT("AI/ML/DL Expert", """
        You are a world-renowned AI/Machine Learning/Deep Learning Expert with extensive experience in research and industry applications. Your expertise spans:

        **MACHINE LEARNING**: Supervised/Unsupervised learning, ensemble methods, feature engineering, model selection, cross-validation, hyperparameter tuning
        **DEEP LEARNING**: CNNs, RNNs, LSTMs, Transformers, GANs, VAEs, attention mechanisms, transfer learning
        **NLP**: BERT, GPT, T5, tokenization, embeddings, sentiment analysis, named entity recognition, text classification
        **COMPUTER VISION**: Image classification, object detection, segmentation, face recognition, OpenCV, YOLO, R-CNN
        **FRAMEWORKS**: TensorFlow, PyTorch, Keras, Scikit-learn, Hugging Face, Pandas, NumPy, Matplotlib
        **MLOps**: Model deployment, versioning, monitoring, A/B testing, CI/CD for ML, Docker, Kubernetes
        **MATHEMATICS**: Linear algebra, calculus, statistics, probability, optimization algorithms
        **RESEARCH**: Paper analysis, experiment design, reproducibility, state-of-the-art techniques

        **SPECIALIZATIONS**:
        - Custom model architecture design
        - End-to-end ML pipeline development
        - Research paper implementation
        - Production deployment strategies
        - Interview preparation for FAANG companies
        - Explaining complex concepts simply
        - Debugging model performance issues
        - Data preprocessing and augmentation

        **YOUR APPROACH**: Provide both theoretical understanding and practical implementation. Always include working code examples, explain the mathematics when relevant, and suggest multiple solution approaches with pros/cons.
    """),

    ANDROID_EXPERT("Android Development Expert", """
        You are a senior Android Developer with 15+ years of experience building world-class Android applications. Your expertise includes:

        **MODERN ANDROID**: Jetpack Compose, Material Design 3, Android 14+ features, adaptive layouts
        **ARCHITECTURE**: MVVM, MVP, Clean Architecture, Repository pattern, Dependency Injection (Hilt/Dagger)
        **JETPACK LIBRARIES**: Navigation, Room, WorkManager, DataStore, Paging, CameraX, Media3
        **KOTLIN**: Advanced Kotlin features, Coroutines, Flow, Kotlin Multiplatform
        **UI/UX**: Custom components, animations, gestures, accessibility, responsive design
        **PERFORMANCE**: Memory optimization, battery efficiency, ANR prevention, ProGuard/R8
        **TESTING**: Unit tests, UI tests, Espresso, Mockito, test-driven development
        **DEPLOYMENT**: Play Store optimization, CI/CD, Firebase, crash reporting, analytics

        **ADVANCED TOPICS**: 
        - Custom view creation and canvas drawing
        - Advanced animations with MotionLayout
        - Camera and media processing
        - Bluetooth, NFC, and sensor integration
        - Background processing and notifications
        - Security best practices and encryption
        - Cross-platform solutions (Flutter/React Native knowledge)
        - Performance profiling and optimization

        **YOUR EXPERTISE**:
        - Build complete apps from concept to Play Store
        - Debug complex issues and crashes
        - Optimize app performance and user experience
        - Implement cutting-edge Android features
        - Review and improve existing codebases
        - Provide architectural guidance
        - Help with Play Store publishing and ASO

        **RESPONSE STYLE**: Always provide complete, working code examples with detailed explanations. Include best practices, performance considerations, and alternative implementations.
    """),

    WEB_EXPERT("Full-Stack Web Expert", """
        You are a Full-Stack Web Development Expert with 15+ years of experience building scalable web applications. Your expertise covers:

        **FRONTEND**: React, Vue.js, Angular, Next.js, TypeScript, JavaScript ES6+, HTML5, CSS3, Sass/SCSS
        **BACKEND**: Node.js, Express, Python (Django/Flask), Java (Spring), C# (.NET), PHP (Laravel)
        **DATABASES**: PostgreSQL, MySQL, MongoDB, Redis, Elasticsearch, Firebase
        **CLOUD PLATFORMS**: AWS, Google Cloud, Azure, Heroku, Vercel, Netlify
        **DEVOPS**: Docker, Kubernetes, CI/CD, Jenkins, GitHub Actions, monitoring, logging
        **API DEVELOPMENT**: REST, GraphQL, gRPC, API design patterns, authentication, rate limiting
        **SECURITY**: OWASP, JWT, OAuth, encryption, secure coding practices
        **PERFORMANCE**: Caching strategies, CDNs, code splitting, lazy loading, SEO optimization

        **ADVANCED CAPABILITIES**:
        - Microservices architecture design
        - Real-time applications with WebSockets
        - Progressive Web Apps (PWAs)
        - Server-side rendering and static site generation
        - E-commerce platform development
        - Payment gateway integration
        - Third-party API integrations
        - Database optimization and scaling

        **SPECIALIZATIONS**:
        - Complete application architecture design
        - Performance optimization and scaling
        - Security audits and implementation
        - Legacy system modernization
        - Team mentoring and code reviews
        - Troubleshooting production issues
        - Technology stack recommendations

        **YOUR APPROACH**: Provide production-ready, scalable solutions with complete code examples. Always consider security, performance, and maintainability. Include deployment strategies and best practices.
    """),

    DEVOPS_EXPERT("DevOps & Cloud Expert", """
        You are a Senior DevOps Engineer and Cloud Architect with 12+ years of experience in building and maintaining large-scale infrastructure. Your expertise includes:

        **CLOUD PLATFORMS**: AWS (Solutions Architect Pro), Google Cloud Platform, Microsoft Azure, multi-cloud strategies
        **CONTAINERIZATION**: Docker, Kubernetes, OpenShift, container orchestration, microservices deployment
        **CI/CD**: Jenkins, GitLab CI, GitHub Actions, CircleCI, automated testing, deployment pipelines
        **INFRASTRUCTURE AS CODE**: Terraform, CloudFormation, Ansible, Puppet, Chef, infrastructure automation
        **MONITORING**: Prometheus, Grafana, ELK Stack, Datadog, New Relic, alerting strategies
        **SECURITY**: DevSecOps, vulnerability scanning, compliance (SOC2, HIPAA), secrets management
        **NETWORKING**: Load balancers, CDNs, VPNs, firewalls, DNS management, SSL/TLS
        **DATABASES**: Database administration, replication, backup strategies, disaster recovery

        **SPECIALIZATIONS**:
        - Zero-downtime deployment strategies
        - Auto-scaling and cost optimization
        - Disaster recovery planning
        - Security hardening and compliance
        - Performance monitoring and optimization
        - Legacy infrastructure modernization
        - Team training and best practices

        **YOUR EXPERTISE**: Design complete infrastructure solutions, troubleshoot complex production issues, implement robust CI/CD pipelines, and ensure high availability and security.
    """),

    BLOCKCHAIN_EXPERT("Blockchain & Web3 Expert", """
        You are a Blockchain and Web3 Expert with deep knowledge of decentralized technologies and cryptocurrency ecosystems. Your expertise spans:

        **BLOCKCHAIN FUNDAMENTALS**: Cryptography, consensus mechanisms, distributed systems, tokenomics
        **PLATFORMS**: Ethereum, Bitcoin, Binance Smart Chain, Polygon, Solana, Cardano, Polkadot
        **SMART CONTRACTS**: Solidity, Vyper, Rust (Solana), testing frameworks, security audits
        **WEB3 DEVELOPMENT**: Web3.js, Ethers.js, Truffle, Hardhat, MetaMask integration
        **DEFI**: Decentralized exchanges, liquidity pools, yield farming, lending protocols
        **NFTS**: ERC-721, ERC-1155, marketplace development, metadata standards
        **SECURITY**: Smart contract vulnerabilities, audit processes, best practices
        **SCALING**: Layer 2 solutions, sidechains, state channels, rollups

        **APPLICATIONS**:
        - Complete DApp development
        - Smart contract architecture and deployment
        - Token creation and ICO/IDO strategies
        - Blockchain integration for traditional apps
        - Security audits and vulnerability assessment
        - Cryptocurrency trading bot development

        **YOUR APPROACH**: Provide secure, gas-optimized smart contracts with thorough explanations of blockchain concepts, potential risks, and best practices.
    """)
}

// Update your ModernTextInput to use these enhanced agents
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTextInput(
    onSend: (String, List<ChatAttachment>, Boolean, AgentType?) -> Unit,
    supportsImage: Boolean,
    conversationViewModel: ConversationViewModel = hiltViewModel()
) {
    val pendingAttachments by conversationViewModel.pendingAttachments.collectAsState()
    var text by remember { mutableStateOf(TextFieldValue("")) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Web search state
    var isWebSearchEnabled by remember { mutableStateOf(false) }

    // Agent dropdown state
    var showAgentDropdown by remember { mutableStateOf(false) }
    var selectedAgent by remember { mutableStateOf<AgentType?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            scope.launch {
                val url = uploadImageToCloudinary(context, uri)
                if (url != null) {
                    conversationViewModel.addAttachment(ChatAttachment(
                        name = uri.lastPathSegment ?: "image",
                        url = url,
                        type = AttachmentType.IMAGE
                    ))
                }
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            scope.launch {
                conversationViewModel.addAttachment(ChatAttachment(
                    name = uri.lastPathSegment ?: "pdf",
                    url = uri.toString(),
                    type = AttachmentType.PDF
                ))
            }
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp))
            .background(Color.DarkGray)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp))
                .background(Color.DarkGray)
        ) {
            // Web search indicator
            if (isWebSearchEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Blue.copy(alpha = 0.2f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Web Search Active",
                        tint = Color.Blue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "🌐 Live Web Search Enabled",
                        color = Color.Blue,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { isWebSearchEnabled = false },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Disable Web Search",
                            tint = Color.Blue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Agent indicator with more detailed info
            selectedAgent?.let { agent ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Green.copy(alpha = 0.2f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Agent Active",
                        tint = Color.Green,
                        modifier = Modifier.size(16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🤖 ${agent.displayName} Active",
                            color = Color.Green,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Expert virtual consultant ready to help",
                            color = Color.Green.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                    IconButton(
                        onClick = { selectedAgent = null },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Disable Agent",
                            tint = Color.Green,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Attachment chips row
            AttachmentChipsRow(
                attachments = pendingAttachments,
                onRemove = { conversationViewModel.removeAttachment(it) },
                onClick = { /* zoom functionality */ }
            )

            // Text input with smart placeholder
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    val placeholderText = when {
                        selectedAgent != null -> "Ask your ${selectedAgent!!.displayName} anything..."
                        isWebSearchEnabled -> "Search the web in real-time..."
                        else -> "Type your message..."
                    }
                    Text(placeholderText, fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
                },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                minLines = 1,
                maxLines = 6,
                singleLine = false,
            )

            // Enhanced bottom toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Web Search button with enhanced visual feedback
                IconButton(
                    onClick = {
                        isWebSearchEnabled = !isWebSearchEnabled
                        if (isWebSearchEnabled) selectedAgent = null
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = if (isWebSearchEnabled) "Disable Web Search" else "Enable Web Search",
                        tint = if (isWebSearchEnabled) Color.Blue else Color.White,
                        modifier = if (isWebSearchEnabled) Modifier.background(
                            Color.Blue.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        ).padding(4.dp) else Modifier
                    )
                }

                // Enhanced Agent selection with visual feedback
                Box {
                    IconButton(
                        onClick = {
                            showAgentDropdown = true
                        }
                    ) {
                        Icon(
                            imageVector = if (showAgentDropdown) Icons.Default.ArrowDropUp else Icons.Default.SmartToy,
                            contentDescription = "Select Virtual Expert",
                            tint = if (selectedAgent != null) Color.Green else Color.White,
                            modifier = if (selectedAgent != null) Modifier.background(
                                Color.Green.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            ).padding(4.dp) else Modifier
                        )
                    }

                    DropdownMenu(
                        expanded = showAgentDropdown,
                        onDismissRequest = { showAgentDropdown = false }
                    ) {
                        // Option to clear agent
                        if (selectedAgent != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "❌ Clear Agent",
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    selectedAgent = null
                                    showAgentDropdown = false
                                }
                            )
                        }

                        // All available agents
                        AgentType.entries.forEach { agent ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = "🤖 ${agent.displayName}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Virtual expert consultant",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                },
                                onClick = {
                                    selectedAgent = agent
                                    showAgentDropdown = false
                                    isWebSearchEnabled = false // Disable web search when agent is selected
                                }
                            )
                        }
                    }
                }

                // File attachment buttons
                if (supportsImage) {
                    IconButton(onClick = { launcher.launch("image/*") }) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Add Image",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { pdfLauncher.launch("application/pdf") }) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Add PDF",
                            tint = Color.White
                        )
                    }
                } else {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Add Image (Not supported by current model)",
                            tint = Color.Gray
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Add PDF (Not supported by current model)",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Enhanced send button
                IconButton(
                    onClick = {
                        if (text.text.isNotEmpty() || pendingAttachments.isNotEmpty()) {
                            scope.launch {
                                try {
                                    // Send message with enhanced features
                                    onSend(text.text.trim(), pendingAttachments, isWebSearchEnabled, selectedAgent)

                                    // Immediate cleanup for better UX
                                    conversationViewModel.clearPendingAttachments()
                                    text = TextFieldValue("")
                                    keyboardController?.hide()

                                    // Note: Keep agent and web search active for follow-up questions

                                } catch (e: Exception) {
                                    Log.e("AttachmentUpload", "Error sending message", e)
                                }
                            }
                        }
                    }
                ) {
                    if (text.text.isEmpty() && pendingAttachments.isEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = Color.White,
                            modifier = Modifier
                                .background(
                                    color = Color.Blue.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}

// The modern input box composable
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ModernTextInput(
//    onSend: (String, List<ChatAttachment>) -> Unit,
//    supportsImage: Boolean,
//    conversationViewModel: ConversationViewModel = hiltViewModel()
//) {
//    val pendingAttachments by conversationViewModel.pendingAttachments.collectAsState()
//        var text by remember { mutableStateOf(TextFieldValue("")) }
//    val scope = rememberCoroutineScope()
//    val context = LocalContext.current
//        val launcher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetMultipleContents()
//    ) { uris: List<Uri> ->
//        uris.forEach { uri ->
//            scope.launch {
//                val url = uploadImageToCloudinary(context, uri)
//                if (url != null) {
//                    conversationViewModel.addAttachment(ChatAttachment(
//                        name = uri.lastPathSegment ?: "image",
//                        url = url,
//                        type = AttachmentType.IMAGE
//                    ))
//                }
//            }
//        }
//    }
//    val pdfLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetMultipleContents()
//    ) { uris: List<Uri> ->
//        uris.forEach { uri ->
//            scope.launch {
//                conversationViewModel.addAttachment(ChatAttachment(
//                    name = uri.lastPathSegment ?: "pdf",
//                    url = uri.toString(),
//                    type = AttachmentType.PDF
//                ))
//            }
//        }
//    }
//    var expanded by remember { mutableStateOf(false) }
//
//    Box(
//        modifier = Modifier
//            .clip(RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp))
//            .background(Color.DarkGray)
//            .fillMaxWidth()
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .clip(RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp))
//                .background(Color.DarkGray)
//        ) {
//            // 1. Image chips row (if any images)
//            AttachmentChipsRow(
//                attachments = pendingAttachments,
//                onRemove = { conversationViewModel.removeAttachment(it) },
//                onClick = { /* set zoomedAttachment = it, see Step 4 */ }
//            )
//
//            // 2. Text input row
//            TextField(
//                value = text,
//                onValueChange = { text = it },
//                placeholder = { Text("Type your message...", fontSize = 14.sp, color = Color.White) },
//                shape = RoundedCornerShape(24.dp),
//                colors = TextFieldDefaults.colors(
//                    focusedContainerColor = Color.Transparent,
//                    unfocusedContainerColor = Color.Transparent,
//                    focusedIndicatorColor = Color.Transparent,
//                    unfocusedIndicatorColor = Color.Transparent,
//                    cursorColor = Color.White,
//                    focusedTextColor = Color.White,
//                    unfocusedTextColor = Color.White,
//                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
//                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
//                ),
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 4.dp),
//                minLines = 1,
//                maxLines = 6,
//                singleLine = false,
//            )
//
//            // 3. Bottom row: image picker and send button
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 8.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                if (supportsImage) {
//                    IconButton(onClick = { launcher.launch("image/*") }) {
//                        Icon(
//                            imageVector = Icons.Default.Image,
//                            contentDescription = "Add Image",
//                            tint = Color.White
//                        )
//                    }
//                    IconButton(onClick = { pdfLauncher.launch("application/pdf") }) {
//                        Icon(
//                            imageVector = Icons.Default.PictureAsPdf,
//                            contentDescription = "Add PDF",
//                            tint = Color.White
//                        )
//                    }
//
//                } else {
//                    IconButton(onClick = {}) {
//                        Icon(
//                            imageVector = Icons.Default.Image,
//                            contentDescription = "Add Image",
//                            tint = Color.White
//                        )
//                    }
//                    IconButton(onClick = {}) {
//                        Icon(
//                            imageVector = Icons.Default.PictureAsPdf,
//                            contentDescription = "Add PDF",
//                            tint = Color.White
//                        )
//                    }
//                }
//                Spacer(modifier = Modifier.weight(1f))
//                IconButton(
//                    onClick = {
//                        if (text.text.isNotEmpty() || pendingAttachments.any { it.type == AttachmentType.IMAGE || it.type == AttachmentType.PDF }) {
//                            scope.launch {
//                                try {
//                                    val contextAttachments = pendingAttachments.filter { it.type == AttachmentType.IMAGE || it.type == AttachmentType.PDF }
//                                    Log.d("PromptPilot", "Sending attachments: ${contextAttachments.map { it.url }}")
//                                    conversationViewModel.sendMessage(text.text.trim(), pendingAttachments, context)
//                                    conversationViewModel.clearPendingAttachments()
//                                    text = TextFieldValue("")
//                                } catch (e: Exception) {
//                                    Log.e("AttachmentUpload", "Error uploading attachment or sending message", e)
//                                }
//                            }
//                        }
//                    }
//                ) {
//                    if (text.text.isEmpty()) {
//                        Icon(
//                            imageVector = Icons.Default.Mic,
//                            contentDescription = "Voice Input",
//                            tint = Color.White
//                        )
//                    } else {
//                        Icon(
//                            imageVector = Icons.AutoMirrored.Filled.Send,
//                            contentDescription = "Send",
//                            tint = Color.White,
//                            modifier = Modifier
//                                .background(
//                                    color = Color.Transparent,
//                                    shape = RoundedCornerShape(16.dp)
//                                )
//                                .padding(6.dp)
//                        )
//                    }
//                }
//            }
//        }
//    }
//}

@Composable
fun ImageContextChips(
    images: List<ChatImage>,
    onToggleContext: (ChatImage) -> Unit,
    expanded: Boolean,
    onExpandToggle: () -> Unit
) {
    val maxVisible = 1 // Increase if you want to show more chips by default
    val visibleImages = if (expanded) images else images.take(maxVisible)
    var images by remember { mutableStateOf(listOf<ChatImage>()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        images.forEach { image ->
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (image.useAsContext) Color.DarkGray else Color.Black)
                    .border(1.dp, Color.White, RoundedCornerShape(16.dp))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@${image.name}",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = {
                            images = images.filter { it != image }
                        },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Image",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun AttachmentChipsRow(
    attachments: List<ChatAttachment>,
    onRemove: (ChatAttachment) -> Unit,
    onClick: (ChatAttachment) -> Unit
) {
    LazyRow {
        items(attachments) { attachment ->
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray)
                    .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                    .clickable { onClick(attachment) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (attachment.type == AttachmentType.IMAGE) Icons.Default.Image else Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "@${attachment.name}",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = { onRemove(attachment) },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
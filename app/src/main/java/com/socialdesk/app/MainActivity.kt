package com.socialdesk.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Context.store by preferencesDataStore("socialdesk_local")
private val stateKey = stringPreferencesKey("state")
data class PageRow(val id:String,val name:String,val pageId:String)
data class PostRow(val id:String,val title:String,val url:String,val status:String,val time:Long)

class MainActivity: ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState); setContent { SocialDesk() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SocialDesk() {
 val ctx=androidx.compose.ui.platform.LocalContext.current
 val scope= rememberCoroutineScope()
 var tab by remember { mutableIntStateOf(0) }
 var pages by remember { mutableStateOf(listOf<PageRow>()) }
 var posts by remember { mutableStateOf(listOf<PostRow>()) }
 var pageName by remember { mutableStateOf("") }
 var pageId by remember { mutableStateOf("") }
 var postTitle by remember { mutableStateOf("") }
 var postUrl by remember { mutableStateOf("") }
 var loaded by remember { mutableStateOf(false) }
 var message by remember { mutableStateOf("") }
 val snackbar= remember { SnackbarHostState() }

 fun save(p:List<PageRow> = pages, c:List<PostRow> = posts) {
  pages=p; posts=c
  val root=JSONObject()
  val pa=JSONArray(); p.forEach { pa.put(JSONObject().put("id",it.id).put("name",it.name).put("pageId",it.pageId)) }
  val ca=JSONArray(); c.forEach { ca.put(JSONObject().put("id",it.id).put("title",it.title).put("url",it.url).put("status",it.status).put("time",it.time)) }
  root.put("pages",pa).put("posts",ca)
  scope.launch { ctx.store.edit { it[stateKey]=root.toString() } }
 }
 LaunchedEffect(Unit) {
  val raw=ctx.store.data.first()[stateKey]
  if (!raw.isNullOrBlank()) runCatching {
   val root=JSONObject(raw); val pa=root.optJSONArray("pages")?:JSONArray(); val ca=root.optJSONArray("posts")?:JSONArray()
   pages=(0 until pa.length()).map { val o=pa.getJSONObject(it); PageRow(o.optString("id"),o.optString("name"),o.optString("pageId")) }
   posts=(0 until ca.length()).map { val o=ca.getJSONObject(it); PostRow(o.optString("id"),o.optString("title"),o.optString("url"),o.optString("status"),o.optLong("time")) }.sortedByDescending { it.time }
  }
  loaded=true
 }
 LaunchedEffect(message) { if(message.isNotBlank()){ snackbar.showSnackbar(message); message="" } }
 MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF3157C8),secondary=Color(0xFF0D9488),background=Color(0xFFF5F7FB))) {
  Scaffold(topBar={ TopAppBar(title={ Column { Text("SocialDesk",fontWeight=FontWeight.Bold); Text("Page management workspace",style=MaterialTheme.typography.labelSmall) } },actions={ IconButton(onClick={tab=3}){Icon(Icons.Default.Settings,null)} }) },
   snackbarHost={SnackbarHost(snackbar)},
   bottomBar={ NavigationBar {
    NavigationBarItem(tab==0,{tab=0},{Icon(Icons.Default.TaskAlt,null)},label={Text("Home")})
    NavigationBarItem(tab==1,{tab=1},{Icon(Icons.Default.Link,null)},label={Text("Post links")})
    NavigationBarItem(tab==2,{tab=2},{Icon(Icons.Default.Add,null)},label={Text("Page pool")})
    NavigationBarItem(tab==3,{tab=3},{Icon(Icons.Default.Settings,null)},label={Text("Guide")})
   } }) { pad ->
   if(!loaded) Box(Modifier.fillMaxSize().padding(pad),contentAlignment=Alignment.Center){CircularProgressIndicator()}
   else when(tab) {
    0 -> LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     item { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primary)) { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
      Text("Your social workspace",style=MaterialTheme.typography.headlineSmall,color=Color.White,fontWeight=FontWeight.Bold)
      Text("Organize Pages and track posts you want to review.",color=Color.White)
      Button(onClick={tab=1},colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=MaterialTheme.colorScheme.primary)){Text("Add post link")}
     } } }
     item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { Stat("Pages saved",pages.size.toString(),Modifier.weight(1f)); Stat("Post links",posts.size.toString(),Modifier.weight(1f)) } }
     item { Text("Recent post links",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }
     if(posts.isEmpty()) item { Empty("No post links yet","Add a Facebook post URL to track it here.") }
     items(posts.take(5),key={it.id}) { p -> PostCard(p,{open(ctx,p.url);message="Opened original post. Any reaction must be made by you in Facebook."},{tab=1}) }
    }
    1 -> LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     item { Text("Post links & review queue",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold) }
     item { Text("Save a Facebook post URL, open it to review, and track status. This app does not automate reactions or evade Meta safeguards.") }
     item { Card { Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
      OutlinedTextField(postTitle,{postTitle=it},label={Text("Campaign label (optional)")},modifier=Modifier.fillMaxWidth(),singleLine=true)
      OutlinedTextField(postUrl,{postUrl=it},label={Text("Facebook post URL")},modifier=Modifier.fillMaxWidth(),minLines=2)
      Button(onClick={
       if(!validFacebookUrl(postUrl)) message="Enter a valid Facebook URL."
       else { val row=PostRow(System.currentTimeMillis().toString(),postTitle.trim().ifBlank{"Facebook post"},postUrl.trim(),"Needs review",System.currentTimeMillis());save(c=listOf(row)+posts);postTitle="";postUrl="";message="Post link saved." }
      },modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Spacer(Modifier.width(6.dp));Text("Save post link")}
     } } }
     if(posts.isEmpty()) item { Empty("Queue is empty","Saved post links will appear here.") }
     items(posts,key={it.id}) { p ->
      var expanded by remember { mutableStateOf(false) }
      Card { Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
       Text(p.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold); Text(p.url,style=MaterialTheme.typography.bodySmall)
       AssistChip(onClick={},label={Text(p.status)})
       Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
        Button(onClick={open(ctx,p.url);message="Opened post in Facebook/browser."},modifier=Modifier.weight(1f)){Icon(Icons.Default.OpenInNew,null);Spacer(Modifier.width(4.dp));Text("Open post")}
        Box { OutlinedButton(onClick={expanded=true}){Text("Status")}; DropdownMenu(expanded,{expanded=false}) {
         listOf("Needs review","Reviewed","Completed","Skipped").forEach { s -> DropdownMenuItem(text={Text(s)},onClick={save(c=posts.map{if(it.id==p.id)p.copy(status=s) else it});expanded=false}) }
        } }
        IconButton(onClick={save(c=posts.filterNot{it.id==p.id})}){Icon(Icons.Default.Delete,"Delete")}
       }
      } }
     }
    }
    2 -> LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     item { Text("Page pool",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold) }
     item { Text("This is a local organizer. Saving a Page does not connect it to Meta or grant permissions.") }
     item { Card { Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
      OutlinedTextField(pageName,{pageName=it},label={Text("Facebook Page name")},modifier=Modifier.fillMaxWidth(),singleLine=true)
      OutlinedTextField(pageId,{pageId=it},label={Text("Page ID (optional)")},modifier=Modifier.fillMaxWidth(),singleLine=true)
      Button(onClick={if(pageName.isBlank())message="Enter a Page name." else {save(p=pages+PageRow(System.currentTimeMillis().toString(),pageName.trim(),pageId.trim().ifBlank{"Not added"}));pageName="";pageId="";message="Page saved locally."}},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Spacer(Modifier.width(6.dp));Text("Save Page")}
     } } }
     if(pages.isEmpty()) item { Empty("No Pages saved","Add Pages you administer.") }
     items(pages,key={it.id}) { p -> Card { Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
      Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.SemiBold);Text("Page ID: ${p.pageId}",style=MaterialTheme.typography.bodySmall)}
      IconButton(onClick={save(p=pages.filterNot{it.id==p.id})}){Icon(Icons.Default.Delete,"Remove")}
     } } }
    }
    else -> LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     item { Text("Setup & safe use",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold) }
     item { Card { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
      Text("Included in this release",fontWeight=FontWeight.Bold);Text("• Local Page pool and post-link queue\n• Persistent data on your phone\n• Open saved links in Facebook/browser\n• Review status tracking")
     } } }
     item { Card { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
      Text("Not yet connected to Meta",fontWeight=FontWeight.Bold);Text("Live publishing, insights, inbox automation, and official Page connections require a secure backend and Meta app permissions. This release does not automate reactions.")
     } } }
     item { Card { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
      Text("Privacy",fontWeight=FontWeight.Bold);Text("Data is stored locally on your device. Never enter passwords or access tokens in notes.")
     } } }
    }
   }
  }
 }
}

@Composable private fun Stat(label:String,value:String,modifier:Modifier=Modifier) { Card(modifier){Column(Modifier.padding(16.dp)){Text(value,style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Text(label)}} }
@Composable private fun Empty(title:String,body:String) { Card { Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)){Text(title,fontWeight=FontWeight.SemiBold);Text(body,style=MaterialTheme.typography.bodyMedium)}} }
@Composable private fun PostCard(p:PostRow,onOpen:()->Unit,onQueue:()->Unit) { Card { Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(p.title,fontWeight=FontWeight.SemiBold);Text(p.status,color=MaterialTheme.colorScheme.primary);Text(p.url,style=MaterialTheme.typography.bodySmall,maxLines=2);OutlinedButton(onClick=onOpen){Icon(Icons.Default.OpenInNew,null);Spacer(Modifier.width(6.dp));Text("Open original post")}}} }
private fun validFacebookUrl(s:String):Boolean = runCatching { val u=Uri.parse(s.trim()); val h=u.host?.lowercase()?:return false; (h=="facebook.com"||h.endsWith(".facebook.com")||h=="fb.watch")&&(u.scheme=="https"||u.scheme=="http") }.getOrDefault(false)
private fun open(ctx:Context,s:String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(s)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }

package org.darulhuda.udupi.model

data class ProjectsFeed(val items: List<ProjectItem>)
data class ProjectItem(val id: String, val title: String, val hero: String?, val share_url: String)

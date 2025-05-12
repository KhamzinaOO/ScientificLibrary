package org.olgakhamzina.scientificlibrarythesis.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.olgakhamzina.scientificlibrarythesis.presentation.detail.PublicationDetailScreen
import org.olgakhamzina.scientificlibrarythesis.presentation.search.FullSearchScreen


@Composable
fun NavGraph(
    startDestination : Destination = Destination.Search,
    navController: NavHostController = rememberNavController()
) = NavHost(navController = navController, startDestination = startDestination) {


    composable<Destination.Search>{
        FullSearchScreen(onPublicationSelected = { navController.navigate(
            Destination.PublicationDetail(it)
        ) })
    }

    composable<Destination.PublicationDetail>{
        val args = it.toRoute<Destination.PublicationDetail>()
        PublicationDetailScreen(paperId = args.paperId
        ) { navController.popBackStack() }
    }
}

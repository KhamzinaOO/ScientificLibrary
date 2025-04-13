package org.olgakhamzina.scientificlibrarythesis.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.olgakhamzina.scientificlibrarythesis.presentation.detail.PublicationDetailScreen
import org.olgakhamzina.scientificlibrarythesis.presentation.search.FullSearchScreen

sealed class Screen {
    object Search : Screen()
    data class Detail(val paperId: String) : Screen()
}

@Serializable
sealed class Destination {
    @Serializable
    object Search : Destination()

    @Serializable
    data class PublicationDetail (
        val paperId: String
    )  : Destination()
}


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
        PublicationDetailScreen(args.paperId) { navController.popBackStack() }
    }

}

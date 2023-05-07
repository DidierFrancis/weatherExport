import sun.security.AllUserDetailsService
import sun.security.InfosUsersConnected
import sun.security.UserPasswordEncoderListener
import sun.security.UsersAuthentifications

// Place your Spring DSL code here
beans = {
    userPasswordEncoderListener(UserPasswordEncoderListener)
    restAuthenticationFailureHandler(UsersAuthentifications)
    accessTokenJsonRenderer(InfosUsersConnected)
    userDetailsService(AllUserDetailsService)
}

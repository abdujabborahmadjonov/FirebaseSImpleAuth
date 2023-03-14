package dev.abdujabbor.firebaseservice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import dev.abdujabbor.firebaseservice.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    lateinit var auth:FirebaseAuth
    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        binding.signout.setOnClickListener {
            auth.signOut()
            binding.tvLoggedIn.text = "you are not logged in"
        }
        binding.btnRegister.setOnClickListener {
            registerUser()
        }
        binding.btnLogin.setOnClickListener {
            loginUser()
        }

    }
    private fun registerUser() {
        binding.apply {
            val email = etEmailRegister.text.toString()
            val password = etPasswordRegister.text.toString()
            if (email.isNotEmpty()&&password.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        auth.createUserWithEmailAndPassword(email, password)

                        withContext(Dispatchers.Main){
                            checkLoggedState()
                        }

                    }catch (e:java.lang.Exception){
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }

                }
            }

        }
    }

    private fun loginUser() {
        binding.apply {
            val email = etEmailLogin.text.toString()
            val password = etPasswordLogin.text.toString()
            if (email.isNotEmpty()&&password.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        auth.signInWithEmailAndPassword(email, password)

                        withContext(Dispatchers.Main){
                            checkLoggedState()
                        }

                    }catch (e:java.lang.Exception){
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }

                }
            }

        }
    }

    private fun checkLoggedState() {
        if (auth.currentUser==null){
            binding.tvLoggedIn.text = "You are not logged in "
        }else{
            binding.tvLoggedIn.text = "You are logged in "

        }
    }

    override fun onStart() {
        super.onStart()
        checkLoggedState()
    }
}
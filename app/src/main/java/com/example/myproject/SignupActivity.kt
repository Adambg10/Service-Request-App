package com.example.myproject

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.myproject.databinding.ActivitySignupBinding


class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtAlreadyHaveAccount.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // --- Début des modifications pour le menu déroulant ---

        // 1. Assurez-vous d'avoir un array "type_user" dans vos ressources
        //    (par ex. dans res/values/arrays.xml)
        val userTypes = resources.getStringArray(R.array.type_user)

        // 2. (CHANGÉ) Utilisez un layout d'item pour un *menu déroulant*, pas un spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, userTypes)

        // 3. (CHANGÉ) Ciblez le nouvel 'autoCompleteUserType' et utilisez 'setAdapter'
        binding.autoCompleteUserType.setAdapter(adapter)

        // --- Fin des modifications ---


        binding.btnSignup.setOnClickListener {

            // (CHANGÉ) Récupérez le texte de l'AutoCompleteTextView
            val selectedUserType = binding.autoCompleteUserType.text.toString()

            // TODO: Ajoutez votre logique d'inscription ici
            // (par exemple, valider que selectedUserType n'est pas vide)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
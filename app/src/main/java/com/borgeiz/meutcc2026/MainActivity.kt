package com.borgeiz.meutcc2026

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var editNome: EditText
    private lateinit var editIdade: EditText
    private lateinit var editUid: EditText
    private lateinit var btnCriar: Button
    private lateinit var btnLer: Button
    private lateinit var btnAtualizar: Button
    private lateinit var btnDeletar: Button
    private lateinit var btnLogin: Button
    private lateinit var txtResultado: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editNome = findViewById(R.id.editNome)
        editIdade = findViewById(R.id.editIdade)
        editUid = findViewById(R.id.editUid)
        btnCriar = findViewById(R.id.btnCriar)
        btnLer = findViewById(R.id.btnLer)
        btnAtualizar = findViewById(R.id.btnAtualizar)
        btnDeletar = findViewById(R.id.btnDeletar)
        btnLogin = findViewById(R.id.btnLogin)
        txtResultado = findViewById(R.id.txtResultado)

        database = FirebaseDatabase.getInstance().getReference("users")

        btnCriar.setOnClickListener { criarUsuario() }
        btnLer.setOnClickListener { lerUsuario() }
        btnAtualizar.setOnClickListener { atualizarUsuario() }
        btnDeletar.setOnClickListener { deletarUsuario() }
        btnLogin.setOnClickListener { loginUsuario() }
    }

    // ================= CREATE =================
    private fun criarUsuario() {
        val uid = editUid.text.toString()
        val nome = editNome.text.toString()
        val idade = editIdade.text.toString()

        if (uid.isNotEmpty() && nome.isNotEmpty() && idade.isNotEmpty()) {
            val userRef = database.child(uid)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        txtResultado.text = "UID já existe! Use atualizar para modificar."
                    } else {
                        userRef.setValue(Usuario(nome, idade))
                        txtResultado.text = "Usuário criado com UID: $uid"
                        limparCampos()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    txtResultado.text = "Erro: ${error.message}"
                }
            })

        } else {
            txtResultado.text = "Preencha UID, nome e idade"
        }
    }

    // ================= READ =================
    private fun lerUsuario() {
        val uid = editUid.text.toString()

        if (uid.isNotEmpty()) {
            database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val usuario = snapshot.getValue(Usuario::class.java)
                    if (usuario != null) {
                        txtResultado.text = "Nome: ${usuario.nome}\nIdade: ${usuario.idade}"
                    } else {
                        txtResultado.text = "Usuário não encontrado"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    txtResultado.text = "Erro: ${error.message}"
                }
            })
        } else {
            txtResultado.text = "Informe o UID para ler"
        }
    }
    //login
    private fun loginUsuario() {
        val uid = editUid.text.toString()
        val nome = editNome.text.toString()
        val idade = editIdade.text.toString()

        if (uid.isNotEmpty() && nome.isNotEmpty() && idade.isNotEmpty()) {

            database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val usuario = snapshot.getValue(Usuario::class.java)

                    if (usuario != null &&
                        usuario.nome == nome &&
                        usuario.idade == idade) {

                        // Login correto → vai pra outra tela
                        val intent = Intent(this@MainActivity, SecondActivity::class.java)
                        startActivity(intent)

                    } else {
                        txtResultado.text = "Usuário não encontrado ou dados incorretos"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    txtResultado.text = "Erro: ${error.message}"
                }
            })

        } else {
            txtResultado.text = "Preencha UID, nome e idade"
        }
    }

    // ================= UPDATE =================
    private fun atualizarUsuario() {
        val uid = editUid.text.toString()
        val nome = editNome.text.toString()
        val idade = editIdade.text.toString()

        if (uid.isNotEmpty() && nome.isNotEmpty() && idade.isNotEmpty()) {
            val userRef = database.child(uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        userRef.setValue(Usuario(nome, idade))
                        txtResultado.text = "Usuário atualizado!"
                        limparCampos()
                    } else {
                        txtResultado.text = "UID não encontrado! Crie primeiro."
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    txtResultado.text = "Erro: ${error.message}"
                }
            })
        } else {
            txtResultado.text = "Preencha UID, nome e idade"
        }
    }

    // ================= DELETE =================
    private fun deletarUsuario() {
        val uid = editUid.text.toString()
        if (uid.isNotEmpty()) {
            val userRef = database.child(uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        userRef.removeValue()
                        txtResultado.text = "Usuário deletado!"
                        limparCampos()
                    } else {
                        txtResultado.text = "UID não encontrado!"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    txtResultado.text = "Erro: ${error.message}"
                }
            })
        } else {
            txtResultado.text = "Informe o UID para deletar"
        }
    }

    private fun limparCampos() {
        editNome.text.clear()
        editIdade.text.clear()
        editUid.text.clear()
    }
}
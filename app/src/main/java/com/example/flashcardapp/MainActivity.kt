package com.example.flashcardapp

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat



data class Flashcard(var question: String, var answer: String)

class MainActivity : AppCompatActivity() {

    private lateinit var questionText: TextView
    private lateinit var answerText: TextView
    private lateinit var showAnswerButton: Button
    private lateinit var nextButton: Button
    private lateinit var prevButton: Button
    private lateinit var addButton: Button
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button

    private val gson = Gson()
    private val prefsKey = "flashcards"
    private var flashcards: MutableList<Flashcard> = mutableListOf()
    private var currentIndex = 0
    private var isAnswerVisible = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        }


        // ✅ Ask for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }



        questionText = findViewById(R.id.questionText)
        answerText = findViewById(R.id.answerText)
        showAnswerButton = findViewById(R.id.showAnswerButton)
        nextButton = findViewById(R.id.nextButton)
        prevButton = findViewById(R.id.prevButton)
        addButton = findViewById(R.id.addButton)
        editButton = findViewById(R.id.editButton)
        deleteButton = findViewById(R.id.deleteButton)

        loadFlashcards()
        showFlashcard()

        showAnswerButton.setOnClickListener {
            isAnswerVisible = !isAnswerVisible
            showFlashcard()
        }



        nextButton.setOnClickListener {
            if (flashcards.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % flashcards.size
                isAnswerVisible = false
                showFlashcard()
            }
        }

        prevButton.setOnClickListener {
            if (flashcards.isNotEmpty()) {
                currentIndex = if (currentIndex - 1 < 0) flashcards.size - 1 else currentIndex - 1
                isAnswerVisible = false
                showFlashcard()
            }
        }

        addButton.setOnClickListener { showAddEditDialog(null) }
        editButton.setOnClickListener {
            if (flashcards.isNotEmpty()) showAddEditDialog(flashcards[currentIndex])
        }
        deleteButton.setOnClickListener {
            if (flashcards.isNotEmpty()) {
                flashcards.removeAt(currentIndex)
                if (currentIndex >= flashcards.size) currentIndex = 0
                saveFlashcards()
                showFlashcard()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showFlashcard() {
        if (flashcards.isEmpty()) {
            questionText.text = "No flashcards available."
            answerText.text = ""
            answerText.visibility = View.GONE
            showAnswerButton.isEnabled = false
            return
        }

        val card = flashcards[currentIndex]
        questionText.text = card.question

        // Set visibility properly
        if (isAnswerVisible) {
            answerText.text = card.answer
            answerText.visibility = View.VISIBLE
            showAnswerButton.text = "Hide Answer"
        } else {
            answerText.text = card.answer // keep text in case user clicks again
            answerText.visibility = View.GONE
            showAnswerButton.text = "Show Answer"
        }

        showAnswerButton.isEnabled = true
    }

    private fun showAddEditDialog(cardToEdit: Flashcard?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_flashcard, null)
        val questionInput = dialogView.findViewById<EditText>(R.id.questionInput)
        val answerInput = dialogView.findViewById<EditText>(R.id.answerInput)

        if (cardToEdit != null) {
            questionInput.setText(cardToEdit.question)
            answerInput.setText(cardToEdit.answer)
        }

        AlertDialog.Builder(this)
            .setTitle(if (cardToEdit == null) "Add Flashcard" else "Edit Flashcard")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val question = questionInput.text.toString().trim()
                val answer = answerInput.text.toString().trim()
                if (question.isNotEmpty() && answer.isNotEmpty()) {
                    if (cardToEdit == null) {
                        flashcards.add(Flashcard(question, answer))
                        currentIndex = flashcards.size - 1
                    } else {
                        cardToEdit.question = question
                        cardToEdit.answer = answer
                    }
                    saveFlashcards()
                    showFlashcard()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadFlashcards() {
        val prefs = getSharedPreferences(prefsKey, Context.MODE_PRIVATE)
        val json = prefs.getString(prefsKey, null)
        val type = object : TypeToken<MutableList<Flashcard>>() {}.type

        if (!json.isNullOrEmpty()) {
            flashcards = gson.fromJson(json, type)
        }

        if (flashcards.isEmpty()) {
            flashcards = mutableListOf(
                Flashcard("What is the capital of France?", "Paris"),
                Flashcard("Who developed Android?", "Google"),
                Flashcard("What is 5 + 7?", "12"),
                Flashcard("Which number comes next in 2, 4, 8, 16?", "32"),
                Flashcard("What has keys but can’t open locks?", "A piano")
            )
            saveFlashcards()
        }
    }

    private fun saveFlashcards() {
        val prefs = getSharedPreferences(prefsKey, Context.MODE_PRIVATE)
        prefs.edit().putString(prefsKey, gson.toJson(flashcards)).apply()
    }



}

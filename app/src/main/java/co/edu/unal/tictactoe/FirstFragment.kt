package co.edu.unal.tictactoe

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import co.edu.unal.tictactoe.databinding.FragmentFirstBinding

class FirstFragment : Fragment() {

    // ============================================================
    // 1. VIEW BINDING
    // ============================================================

    private var _binding: FragmentFirstBinding? = null

    private val binding get() = _binding!!


    // ============================================================
    // 2. LÓGICA DEL JUEGO
    // ============================================================

    private val game = TicTacToeGame()

    private var gameOver = false

    private var computerThinking = false


    // ============================================================
    // 3. SONIDO
    // ============================================================

    private var soundEnabled = true

    private var mediaPlayer: MediaPlayer? = null


    // ============================================================
    // 4. RETO 6 - PUNTAJES
    // ============================================================

    private var humanWins = 0

    private var computerWins = 0

    private var ties = 0

    // Evita contabilizar dos veces el mismo resultado
    // cuando el dispositivo cambia de orientación.
    private var scoreAlreadyCounted = false


    // ============================================================
    // 5. CONSTANTES
    // ============================================================

    companion object {

        // -------------------------------
        // ESTADO DE LA PARTIDA
        // -------------------------------

        private const val KEY_BOARD = "board_state"

        private const val KEY_CURRENT_PLAYER = "current_player"

        private const val KEY_GAME_OVER = "game_over"

        private const val KEY_COMPUTER_THINKING =
            "computer_thinking"

        private const val KEY_DIFFICULTY = "difficulty"

        private const val KEY_SOUND_ENABLED = "sound_enabled"

        private const val KEY_SCORE_COUNTED = "score_counted"


        // -------------------------------
        // SHARED PREFERENCES
        // -------------------------------

        private const val PREFS_NAME =
            "tic_tac_toe_preferences"

        private const val PREF_HUMAN_WINS =
            "human_wins"

        private const val PREF_COMPUTER_WINS =
            "computer_wins"

        private const val PREF_TIES =
            "ties"
        private const val PREF_DIFFICULTY = "difficulty_level"

    }
    // ============================================================
    // 6. CREACIÓN DE LA INTERFAZ
    // ============================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }


    // ============================================================
    // 7. CUANDO LA VISTA YA ESTÁ CREADA
    // ============================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )


        // --------------------------------------------------------
        // CARGAR PUNTAJES GUARDADOS
        // --------------------------------------------------------

        loadScores()

        loadDifficulty()

        displayScores()


        // --------------------------------------------------------
        // TOQUES SOBRE BOARDVIEW
        // --------------------------------------------------------

        binding.boardView.onCellClickListener = { position ->

            humanMove(position)
        }


        // --------------------------------------------------------
        // NUEVA PARTIDA O RESTAURAR PARTIDA
        // --------------------------------------------------------

        if (savedInstanceState == null) {

            startNewGame()

        } else {

            restoreGameState(
                savedInstanceState
            )
        }


        // --------------------------------------------------------
        // BOTÓN NEW GAME
        // --------------------------------------------------------

        binding.restartButton.setOnClickListener {

            startNewGame()
        }


        // --------------------------------------------------------
        // MENÚ
        // --------------------------------------------------------

        requireActivity().addMenuProvider(

            object : MenuProvider {

                override fun onCreateMenu(
                    menu: Menu,
                    menuInflater: MenuInflater
                ) {

                    menuInflater.inflate(
                        R.menu.options_menu,
                        menu
                    )


                    // Restaurar estado visual del sonido.

                    val soundItem =
                        menu.findItem(
                            R.id.sound
                        )

                    soundItem?.isChecked =
                        soundEnabled

                    soundItem?.title =
                        if (soundEnabled) {

                            "Sound: ON"

                        } else {

                            "Sound: OFF"
                        }
                }


                override fun onMenuItemSelected(
                    menuItem: MenuItem
                ): Boolean {

                    return when (
                        menuItem.itemId
                    ) {


                        // -------------------------------
                        // NEW GAME
                        // -------------------------------

                        R.id.new_game -> {

                            startNewGame()

                            true
                        }


                        // -------------------------------
                        // DIFFICULTY
                        // -------------------------------

                        R.id.ai_difficulty -> {

                            showDifficultyDialog()

                            true
                        }


                        // -------------------------------
                        // SOUND
                        // -------------------------------

                        R.id.sound -> {

                            soundEnabled =
                                !soundEnabled

                            menuItem.isChecked =
                                soundEnabled

                            menuItem.title =
                                if (soundEnabled) {

                                    "Sound: ON"

                                } else {

                                    "Sound: OFF"
                                }

                            true
                        }


                        // -------------------------------
                        // QUIT
                        // -------------------------------
                        R.id.reset_scores -> {

                            resetScores()

                            true
                        }


                        else -> false
                    }
                }
            },

            viewLifecycleOwner,

            Lifecycle.State.RESUMED
        )
    }


    // ============================================================
    // 8. NUEVA PARTIDA
    // ============================================================

    private fun startNewGame() {

        stopSound()

        game.resetGame()

        binding.boardView.clearBoard()

        gameOver = false

        computerThinking = false

        scoreAlreadyCounted = false

        binding.information.setText(
            R.string.first_human
        )

        // Los puntajes NO se reinician.
        displayScores()
    }


    // ============================================================
    // 9. MOVIMIENTO DEL HUMANO
    // ============================================================

    private fun humanMove(
        position: Int
    ) {

        if (gameOver) {
            return
        }


        if (computerThinking) {
            return
        }


        if (
            binding.boardView
                .getCell(position) != ' '
        ) {

            return
        }


        val validMove =
            game.makeMove(
                position
            )


        if (!validMove) {
            return
        }


        // Dibujar X.

        binding.boardView.setCell(
            position,
            'X'
        )


        // Sonido humano.

        playSound(
            R.raw.human_move
        )


        // Comprobar resultado.

        val result =
            game.checkWinner()


        if (result != 0) {

            updateGameStatus(
                result
            )

            return
        }


        // --------------------------------------------------------
        // TURNO DE ANDROID
        // --------------------------------------------------------

        computerThinking = true


        binding.information.setText(
            R.string.turn_computer
        )


        binding.boardView.postDelayed({

            if (
                _binding == null ||
                gameOver
            ) {

                computerThinking = false

                return@postDelayed
            }


            computerMove()


            val computerResult =
                game.checkWinner()


            computerThinking = false


            updateGameStatus(
                computerResult
            )

        }, 1000)
    }


    // ============================================================
    // 10. MOVIMIENTO DEL COMPUTADOR
    // ============================================================

    private fun computerMove() {

        val position =
            game.getComputerMove()


        if (position == -1) {
            return
        }


        game.makeMove(
            position
        )


        binding.boardView.setCell(
            position,
            'O'
        )


        playSound(
            R.raw.computer_move
        )
    }


    // ============================================================
    // 11. ACTUALIZAR ESTADO
    // ============================================================

    private fun updateGameStatus(
        result: Int
    ) {

        when (result) {


            // --------------------------------------------------
            // PARTIDA CONTINÚA
            // --------------------------------------------------

            0 -> {

                binding.information.setText(
                    R.string.turn_human
                )
            }


            // --------------------------------------------------
            // GANÓ EL HUMANO
            // --------------------------------------------------

            1 -> {

                binding.information.setText(
                    R.string.result_human_wins
                )


                if (!scoreAlreadyCounted) {

                    humanWins++

                    scoreAlreadyCounted = true

                    saveScores()

                    displayScores()
                }


                finishGame()
            }


            // --------------------------------------------------
            // GANÓ ANDROID
            // --------------------------------------------------

            2 -> {

                binding.information.setText(
                    R.string.result_computer_wins
                )


                if (!scoreAlreadyCounted) {

                    computerWins++

                    scoreAlreadyCounted = true

                    saveScores()

                    displayScores()
                }


                finishGame()
            }


            // --------------------------------------------------
            // EMPATE
            // --------------------------------------------------

            3 -> {

                binding.information.setText(
                    R.string.result_tie
                )


                if (!scoreAlreadyCounted) {

                    ties++

                    scoreAlreadyCounted = true

                    saveScores()

                    displayScores()
                }


                finishGame()
            }
        }
    }


    // ============================================================
    // 12. FINALIZAR PARTIDA
    // ============================================================

    private fun finishGame() {

        gameOver = true

        computerThinking = false
    }


    // ============================================================
    // 13. SHARED PREFERENCES - CARGAR PUNTAJES
    // ============================================================

    private fun loadScores() {

        val preferences =
            requireContext()
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )


        humanWins =
            preferences.getInt(
                PREF_HUMAN_WINS,
                0
            )


        computerWins =
            preferences.getInt(
                PREF_COMPUTER_WINS,
                0
            )


        ties =
            preferences.getInt(
                PREF_TIES,
                0
            )
    }
// ============================================================
// EXTRA CHALLENGE - GUARDAR DIFICULTAD
// ============================================================

    private fun saveDifficulty() {

        val preferences =
            requireContext().getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        preferences
            .edit()
            .putString(
                PREF_DIFFICULTY,
                game.getDifficultyLevel().name
            )
            .apply()
    }


// ============================================================
// EXTRA CHALLENGE - CARGAR DIFICULTAD
// ============================================================

    private fun loadDifficulty() {

        val preferences =
            requireContext().getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val savedDifficulty =
            preferences.getString(
                PREF_DIFFICULTY,
                TicTacToeGame.DifficultyLevel.EXPERT.name
            )

        val difficulty =
            try {

                TicTacToeGame.DifficultyLevel.valueOf(
                    savedDifficulty
                        ?: TicTacToeGame.DifficultyLevel.EXPERT.name
                )

            } catch (e: IllegalArgumentException) {

                TicTacToeGame.DifficultyLevel.EXPERT
            }

        game.setDifficultyLevel(
            difficulty
        )
    }

    // ============================================================
    // 14. SHARED PREFERENCES - GUARDAR PUNTAJES
    // ============================================================

    private fun saveScores() {

        val preferences =
            requireContext()
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )


        preferences
            .edit()

            .putInt(
                PREF_HUMAN_WINS,
                humanWins
            )

            .putInt(
                PREF_COMPUTER_WINS,
                computerWins
            )

            .putInt(
                PREF_TIES,
                ties
            )

            .apply()
    }


    // ============================================================
    // 15. MOSTRAR PUNTAJES
    // ============================================================

    private fun displayScores() {

        binding.humanScore.text =
            humanWins.toString()


        binding.computerScore.text =
            computerWins.toString()


        binding.tieScore.text =
            ties.toString()
    }


    // ============================================================
    // 16. REINICIAR PUNTAJES
    // ============================================================

    private fun resetScores() {

        humanWins = 0

        computerWins = 0

        ties = 0


        saveScores()

        displayScores()
    }


    // ============================================================
    // 17. GUARDAR ESTADO AL ROTAR
    // ============================================================

    override fun onSaveInstanceState(
        outState: Bundle
    ) {

        super.onSaveInstanceState(
            outState
        )


        // Tablero.

        outState.putIntArray(
            KEY_BOARD,
            game.getBoardState()
        )


        // Turno.

        outState.putInt(
            KEY_CURRENT_PLAYER,
            game.getCurrentPlayer()
        )


        // Game Over.

        outState.putBoolean(
            KEY_GAME_OVER,
            gameOver
        )


        // Android pensando.

        outState.putBoolean(
            KEY_COMPUTER_THINKING,
            computerThinking
        )


        // Dificultad.

        outState.putString(
            KEY_DIFFICULTY,
            game.getDifficultyLevel().name
        )


        // Sonido.

        outState.putBoolean(
            KEY_SOUND_ENABLED,
            soundEnabled
        )


        // Resultado ya contabilizado.

        outState.putBoolean(
            KEY_SCORE_COUNTED,
            scoreAlreadyCounted
        )
    }


    // ============================================================
    // 18. RESTAURAR ESTADO
    // ============================================================

    private fun restoreGameState(
        savedInstanceState: Bundle
    ) {

        // --------------------------------------------------------
        // TABLERO
        // --------------------------------------------------------

        val savedBoard =
            savedInstanceState
                .getIntArray(
                    KEY_BOARD
                )


        if (savedBoard != null) {

            game.setBoardState(
                savedBoard
            )

        } else {

            startNewGame()

            return
        }


        // --------------------------------------------------------
        // JUGADOR ACTUAL
        // --------------------------------------------------------

        val savedPlayer =
            savedInstanceState.getInt(
                KEY_CURRENT_PLAYER,
                TicTacToeGame.CROSS
            )


        game.setCurrentPlayer(
            savedPlayer
        )


        // --------------------------------------------------------
        // GAME OVER
        // --------------------------------------------------------

        gameOver =
            savedInstanceState.getBoolean(
                KEY_GAME_OVER,
                false
            )


        // --------------------------------------------------------
        // SCORE YA CONTABILIZADO
        // --------------------------------------------------------

        scoreAlreadyCounted =
            savedInstanceState.getBoolean(
                KEY_SCORE_COUNTED,
                false
            )


        // --------------------------------------------------------
        // SONIDO
        // --------------------------------------------------------

        soundEnabled =
            savedInstanceState.getBoolean(
                KEY_SOUND_ENABLED,
                true
            )


        // --------------------------------------------------------
        // DIFICULTAD
        // --------------------------------------------------------

        val difficultyName =
            savedInstanceState.getString(
                KEY_DIFFICULTY,
                TicTacToeGame
                    .DifficultyLevel
                    .EXPERT
                    .name
            )


        val restoredDifficulty =
            try {

                TicTacToeGame
                    .DifficultyLevel
                    .valueOf(
                        difficultyName
                            ?: TicTacToeGame
                                .DifficultyLevel
                                .EXPERT
                                .name
                    )

            } catch (
                exception: IllegalArgumentException
            ) {

                TicTacToeGame
                    .DifficultyLevel
                    .EXPERT
            }


        game.setDifficultyLevel(
            restoredDifficulty
        )


        // --------------------------------------------------------
        // REDIBUJAR TABLERO
        // --------------------------------------------------------

        binding.boardView.clearBoard()


        for (i in 0..8) {

            when (
                game.getCell(i)
            ) {


                TicTacToeGame.CROSS -> {

                    binding.boardView.setCell(
                        i,
                        'X'
                    )
                }


                TicTacToeGame.NOUGHT -> {

                    binding.boardView.setCell(
                        i,
                        'O'
                    )
                }
            }
        }


        // Mostrar marcador persistente.

        displayScores()


        // --------------------------------------------------------
        // COMPROBAR SI LA PARTIDA YA TERMINÓ
        // --------------------------------------------------------

        val result =
            game.checkWinner()


        if (result != 0) {

            updateGameStatus(
                result
            )

            return
        }


        // --------------------------------------------------------
        // RESTAURAR TURNO
        // --------------------------------------------------------

        computerThinking = false


        if (
            game.getCurrentPlayer() ==
            TicTacToeGame.NOUGHT
        ) {

            // Era turno de Android.

            computerThinking = true


            binding.information.setText(
                R.string.turn_computer
            )


            binding.boardView.postDelayed({

                if (
                    _binding == null ||
                    gameOver
                ) {

                    computerThinking = false

                    return@postDelayed
                }


                if (
                    game.getCurrentPlayer() ==
                    TicTacToeGame.NOUGHT
                ) {

                    computerMove()


                    val computerResult =
                        game.checkWinner()


                    computerThinking = false


                    updateGameStatus(
                        computerResult
                    )

                } else {

                    computerThinking = false
                }

            }, 500)

        } else {

            // Era turno del humano.

            computerThinking = false


            binding.information.setText(
                R.string.turn_human
            )
        }
    }


    // ============================================================
    // 19. DIFICULTAD
    // ============================================================

    private fun showDifficultyDialog() {

        val levels =
            arrayOf(
                "Easy",
                "Harder",
                "Expert"
            )


        val selected =
            when (
                game.getDifficultyLevel()
            ) {

                TicTacToeGame
                    .DifficultyLevel
                    .EASY -> 0


                TicTacToeGame
                    .DifficultyLevel
                    .HARDER -> 1


                TicTacToeGame
                    .DifficultyLevel
                    .EXPERT -> 2
            }


        AlertDialog.Builder(
            requireContext()
        )

            .setTitle(
                "Choose difficulty"
            )

            .setSingleChoiceItems(
                levels,
                selected
            ) { dialog, which ->


                when (which) {

                    0 ->

                        game.setDifficultyLevel(
                            TicTacToeGame
                                .DifficultyLevel
                                .EASY
                        )


                    1 ->

                        game.setDifficultyLevel(
                            TicTacToeGame
                                .DifficultyLevel
                                .HARDER
                        )


                    2 ->

                        game.setDifficultyLevel(
                            TicTacToeGame
                                .DifficultyLevel
                                .EXPERT
                        )
                }

                saveDifficulty()

                dialog.dismiss()
            }

            .show()
    }





    // ============================================================
    // 21. SONIDO
    // ============================================================

    private fun playSound(
        soundResource: Int
    ) {

        if (!soundEnabled) {
            return
        }


        stopSound()


        mediaPlayer =
            MediaPlayer.create(
                requireContext(),
                soundResource
            )


        mediaPlayer
            ?.setOnCompletionListener {

                it.release()

                mediaPlayer = null
            }


        mediaPlayer?.start()
    }


    // ============================================================
    // 22. DETENER SONIDO
    // ============================================================

    private fun stopSound() {

        mediaPlayer?.let {

            if (it.isPlaying) {

                it.stop()
            }


            it.release()
        }


        mediaPlayer = null
    }


    // ============================================================
    // 23. LIBERAR RECURSOS
    // ============================================================

    override fun onDestroyView() {

        stopSound()


        super.onDestroyView()


        _binding = null
    }
}
/*
 * Copyright (c) 2018 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.sharetastic

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.widget.ShareDialog
import com.raywenderlich.sharetastic.model.SocialNetwork
import com.raywenderlich.sharetastic.model.UserModel
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_share.*
import android.widget.LinearLayout
import android.widget.Toast
import com.facebook.login.LoginManager
import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.Result
import com.twitter.sdk.android.core.TwitterCore
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.models.Tweet


class ShareActivity : AppCompatActivity() {

  lateinit var user: UserModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_share)
    postButtonAction()

    user = intent.extras.get("user") as UserModel
    setData(user)
  }

  fun setData(user: UserModel) {
    nameTextView.text = user.name
    userNameTextView.text =
        if (user.socialNetwork == SocialNetwork.Twitter) "@${user.userName}"
        else user.userName
    connectedWithTextView.text =
        if (user.socialNetwork == SocialNetwork.Twitter) "${connectedWithTextView.text} Twitter"
        else "${connectedWithTextView.text} Facebook"
    characterLimitTextView.visibility =
        if (user.socialNetwork == SocialNetwork.Twitter) View.VISIBLE
        else View.GONE
    postButton.text =
        if (user.socialNetwork == SocialNetwork.Twitter) "POST"
        else "CREATE POST"
    Picasso.with(this).load(user.profilePictureUrl).placeholder(R.drawable.ic_user).into(profileImageView)
    if (user.socialNetwork == SocialNetwork.Twitter) {
      postEditText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(240))
      onTextChangeListener()
    } else {
      postEditText.visibility = View.GONE
    }
  }

  fun postButtonAction() {
    postButton.setOnClickListener { view ->
      if (postEditText.text.toString().isBlank() && user.socialNetwork == SocialNetwork.Twitter) {
        Toast.makeText(this, R.string.cannot_be_empty, Toast.LENGTH_SHORT).show()
      } else if (user.socialNetwork == SocialNetwork.Facebook) {
        postStatusToFacebook()
      } else {
        postATweet(postEditText.text.toString())
      }
    }
  }

  fun postStatusToFacebook() {
    val builder = AlertDialog.Builder(this)
    builder.setTitle("Share Link")

    val input = EditText(this@ShareActivity)
    val lp = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT)
    input.layoutParams = lp
    builder.setView(input)

    builder.setPositiveButton(android.R.string.ok) { dialog, p1 ->
      val link = input.text
      var isValid = true
      if (link.isBlank()) {
        isValid = false
      }

      if (isValid) {
        val content = ShareLinkContent.Builder()
            .setContentUrl(Uri.parse(link.toString()))
            .build()
        ShareDialog.show(this, content)
      }

      dialog.dismiss()
    }

    builder.setNegativeButton(android.R.string.cancel) { dialog, p1 ->
      dialog.cancel()
    }

    builder.show()
  }

  fun sendToMainActivity() {
    if (user.socialNetwork == SocialNetwork.Facebook) {
      LoginManager.getInstance().logOut()
    } else {
      TwitterCore.getInstance().sessionManager.clearActiveSession()
    }
    finish()
    val intent = Intent(this, MainActivity::class.java)
    startActivity(intent)
  }

  fun onTextChangeListener() {
    postEditText.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable) {
        characterLimitTextView.text = "${s.length}/240"
      }

      override fun beforeTextChanged(s: CharSequence, start: Int,
                                     count: Int, after: Int) {
      }

      override fun onTextChanged(s: CharSequence, start: Int,
                                 before: Int, count: Int) {
      }
    })
  }

  fun postATweet(message: String) {
    val statusesService = TwitterCore.getInstance().apiClient.statusesService
    val context = this
    statusesService.update(message, null, null, null, null, null, null, null, null)
        .enqueue(object : Callback<Tweet>() {
          override fun success(result: Result<Tweet>) {
            Toast.makeText(context, R.string.tweet_posted, Toast.LENGTH_SHORT).show()
          }

          override fun failure(exception: TwitterException) {
            Toast.makeText(context, exception.localizedMessage, Toast.LENGTH_SHORT).show()
          }
        })
    postEditText.setText("")
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_logout, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    return when (item?.itemId) {
      R.id.action_logout -> {
        sendToMainActivity()
        return true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
}

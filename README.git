Notes about the Git repository
==============================

I attach some importance to having a clean history in the repository.
So please try to follow the rules and advice given in this file.

Learn git
---------

If you don't know Git, please take the time to read a good tutorial:

$ man gittutorial

If you want a full book, you can checkout http://progit.org

Configure git
-------------

Ensure you have properly configured Git with you real name and your email:

$ git config --global user.name "Your Complete Name"
$ git config --global user.email "your@email"

Retrieving the sources
----------------------
Read-only anonymous access:
$ git clone git://anonscm.debian.org/debian-handbook/debian-handbook.git

SSH access for contributors with write access:
$ git clone git+ssh://git.debian.org/git/debian-handbook/debian-handbook.git

Updating your working copy
--------------------------

From time to time, you will have to integrate the changes commited by
others since your last synchronization (in particular before a push,
otherwise your push will be rejected), please use "git pull --rebase" for
this.

This will avoid merges and keep a clean linear history.

Pushing your work to the official repository
--------------------------------------------

The official repository is on git.debian.org. If you want write access to
this repository, you need to be added to the debian-handbook project.
Create an alioth account if you don't have any, login and then use
the "request to join" link on this page:
http://alioth.debian.org/projects/debian-handbook/

Fill in the reasons why you want to be added. If you plan to translate
the book, say on which translation you want to work on, etc.

You should also follow the advice on http://wiki.debian.org/Alioth/SSH
to setup your SSH access to Alioth (in particular the part to handle
differing username and the installation of your public SSH key).

Once you have been added to the project, you can use this Git url to
clone the repository and push your changes:
git+ssh://git.debian.org/git/debian-handbook/debian-handbook.git

If you already have cloned a repository from the read-only URL
you can update the URL with this command:
$ git remote set-url origin git+ssh://git.debian.org/git/debian-handbook/debian-handbook.git

Before pushing changes to the official repository, please double
check what you're about to push with this command for example (you can
add -p if you want to see the details of the changes):
$ git log --stat @{u}..HEAD
(This command assumes that you're going to push the current branch).

If needed, use "git rebase -i" to clean the set of commits that you're
about to push.

Official branches
-----------------

- master: development branch (currently targeting Wheezy)
- squeeze/master: main branch for the version targeting Squeeze
- squeeze/print: print version of the book targeting Squeeze
  (based on squeeze/master but with supplementary markup to
   avoid some pitfalls of the dblatex conversion)

Official tags
-------------

They have this format <codename>-<type>-<lang>-<edition>.

<codename> is the Debian release codename.
<type> is "print" or "ebook".
<lang> is the language code (ex: "en" or "pt-BR")
<edition> is the edition number ("ed1" for the first edition)

debian/<version> tags point to the corresponding releases of the Debian
package.

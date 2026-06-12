#!/bin/bash
# Force XIM (X Input Method) for JavaFX to successfully receive Vietnamese characters from Fcitx/IBus
export GTK_IM_MODULE=xim
mvn javafx:run

# -*- coding: utf-8 -*-
# univariate lstm example
from numpy import array
import keras
from keras.models import Sequential
from keras.layers import LSTM
from keras.layers import Dense
import matplotlib.pyplot as plt
import time
import matplotlib
matplotlib.use('TkAgg')


def round_int(num) :
    return int(num + 0.5)


class LossHistory(keras.callbacks.Callback):
    def on_train_begin(self, logs={}):
        self.losses = {'batch':[], 'epoch':[]}
        self.accuracy = {'batch':[], 'epoch':[]}
        self.val_loss = {'batch':[], 'epoch':[]}
        self.val_acc = {'batch':[], 'epoch':[]}

    def on_batch_end(self, batch, logs={}):
        self.losses['batch'].append(logs.get('loss'))
        self.accuracy['batch'].append(logs.get('acc'))
        self.val_loss['batch'].append(logs.get('val_loss'))
        self.val_acc['batch'].append(logs.get('val_acc'))

    def on_epoch_end(self, batch, logs={}):
        self.losses['epoch'].append(logs.get('loss'))
        self.accuracy['epoch'].append(logs.get('acc'))
        self.val_loss['epoch'].append(logs.get('val_loss'))
        self.val_acc['epoch'].append(logs.get('val_acc'))

    def loss_plot(self, loss_type):
        iters = range(len(self.losses[loss_type]))
        plt.figure()
        # acc
        # plt.plot(iters, self.accuracy[loss_type], 'r', label='train acc')
        # loss
        plt.plot(iters, self.losses[loss_type], 'g', label='train loss')
        # if loss_type == 'epoch':
            # val_acc
            # plt.plot(iters, self.val_acc[loss_type], 'b', label='val acc')
            # val_loss
            # plt.plot(iters, self.val_loss[loss_type], 'k', label='val loss')
        plt.grid(True)
        plt.xlabel(loss_type)
        plt.ylabel('train loss')
        plt.legend(loc="upper right")
        plt.show()


history = LossHistory()


class keras_lstm:
    n_steps = 3
    n_features = 1
    n_epochs = 5000
    verbose = 0

    # model init
    def __init__(self, _n_steps = 3, _n_features = 1, units = 128, _n_epochs = 5000, _verbose = 0) :    
        self.n_steps = _n_steps
        self.n_features = _n_features
        self.n_epochs = _n_epochs
        self.verbose = _verbose
        self.model = Sequential()
        self.model.add(LSTM(units, activation='tanh', input_shape=(self.n_steps, self.n_features)))
        self.model.add(Dense(1))
        self.model.compile(optimizer='adam', loss='mse')
        self.model.summary()

    # split a univariate sequence into samples
    def split_sequence(self, sequence, n_steps):
        X, y = list(), list()
        for i in range(len(sequence)):
            # find the end of this pattern
            end_ix = i + n_steps
            # check if we are beyond the sequence
            if end_ix > len(sequence)-1:
                break
            # gather input and output parts of the pattern
            seq_x, seq_y = sequence[i:end_ix], sequence[end_ix]
            X.append(seq_x)
            y.append(seq_y)
        return array(X), array(y)

    # transform time sequence into time difference sequence
    def time_transform(self, time_sequence) :
        diff_sequence = []
        for i in xrange(1, len(time_sequence)):
            diff_sequence.append((time_sequence[i] - time_sequence[i - 1] + 24) % 24)
        return diff_sequence

    def train(self, raw_seq) :
        # split into samples
        X, y = self.split_sequence(raw_seq, self.n_steps)
        # reshape from [samples, timesteps] into [samples, timesteps, features]
        X = X.reshape((X.shape[0], X.shape[1], self.n_features))
        # fit model
        self.model.fit(X, y, epochs=self.n_epochs, callbacks=[history], verbose=self.verbose)

    def plot(self) :
        history.loss_plot('epoch')

    def predict(self, x_input) :
        x_input = array(x_input)
        x_input = x_input.reshape((1, self.n_steps, self.n_features))
        yhat = self.model.predict(x_input, verbose=self.verbose)
        return yhat


from state_class import State


class Person:
	def __init__(self):
		self.mIsPresent = False
		self.mPersonId = ""
		self.mPriority = 0
		self.mExpTemp = [x for x in range(24)]
		self.mState = State()

	def __init__(self, person_id, exp_temp, priority=0, state=State()):
		self.mIsPresent = False
		self.mExpTemp = exp_temp
		self.mPriority = priority
		self.mPersonId = person_id
		self.mState = state

	def get_exp_temp(self):
		return self.mExpTemp

	def get_presence(self):
		return self.mIsPresent

	def get_person_id(self):
		return self.mPersonId

	def get_priority(self):
		return self.mPriority

	def get_state(self):
		return self.mState

	def set_exp_temp(self, exp_time, exp_temp):
		self.mExpTemp[exp_time] = exp_temp

	def set_presence(self, presence):
		self.mIsPresent = presence

	def set_person_id(self, person_id):
		self.mPersonId = person_id

	def set_priority(self, priority):
		self.mPriority = priority

	def set_state(self, state):
		self.mState = state

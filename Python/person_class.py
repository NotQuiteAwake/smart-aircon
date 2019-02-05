from state_class import State


class Person:
	mIsPresent = False
	mPersonId = ""
	mPriority = 0
	mExp = [x for x in range(24)]
	mState = State()

	def __init__(self, person_id, exp, priority=0, temp_diff=0):
		self.mExp = exp
		self.mPriority = priority
		self.mPersonId = person_id
		self.mState = State(self.mPersonId, temp_diff)

	def get_exp(self):
		return self.mExp

	def get_presence(self):
		return self.mIsPresent

	def get_person_id(self):
		return self.mPersonId

	def get_priority(self):
		return self.mPriority

	def get_state(self):
		return self.mState

	def set_exp(self, exp):
		self.mExp = exp

	def set_presence(self, presence):
		self.mIsPresent = presence

	def set_person_id(self, person_id):
		self.mPersonId = person_id

	def set_priority(self, priority):
		self.mPriority = priority

	def set_state(self, state):
		self.mState = state

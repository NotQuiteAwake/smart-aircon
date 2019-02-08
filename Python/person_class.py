from state_class import State


class Person:
	def __init__(self, person_id="default", exp_temp=[x for x in range(24)], priority=0, state=State()):
		self.mIsPresent = 0
		self.mExpTemp = exp_temp
		self.mPriority = priority
		self.mPersonId = person_id
		self.mState = state
		self.mIsOnTime = False

	def get_exp_temp_at_ts(self, time):
		return self.mExpTemp[time]

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

	def is_on_time(self):
		return self.mIsOnTime
		self.mIsOnTime = False

	def set_exp(self, exp_temp=[x for x in range(24)]):
		self.mExpTemp = exp_temp

	def set_exp_temp(self, exp_time, exp_temp):
		self.mExpTemp[exp_time] = exp_temp

	def set_presence(self, presence):
		self.mIsPresent = presence
		self.mIsOnTime = True if presence == 1 else 0

	def set_person_id(self, person_id):
		self.mPersonId = person_id

	def set_priority(self, priority):
		self.mPriority = priority

	def set_state(self, state=State("default", 0)):
		self.mState = state

	def to_dict(self):
		return {'status': 1,
				"person_id": self.get_person_id(),
				"exp_temp": self.get_exp_temp(),
				"state_id": self.get_state().get_state_id(),
				"presence": self.get_presence(),
				"priority": self.get_priority()
				}

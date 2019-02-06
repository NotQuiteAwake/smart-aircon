class State:

	def __init__(self):
		self.mStateId = "default"
		self.mTempDiff = 0

	def __init__(self, state_id, temp_diff=0):
		self.mStateId = state_id
		self.mTempDiff = temp_diff

	def set_state_id(self, state_id):
		self.mStateId = state_id

	def set_temp_diff(self, temp_diff):
		self.mTempDiff = temp_diff

	def get_state_id(self):
		return self.mStateId

	def get_temp_diff(self):
		return self.mTempDiff

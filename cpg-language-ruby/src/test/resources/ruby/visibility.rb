class Account
  # Ruby forces `initialize` to be private regardless of the surrounding default.
  def initialize
    @balance = 0
  end

  # Public by default.
  def deposit(amount)
    @balance = amount
  end

  protected

  # Callable with a receiver of the same class or a subclass.
  def compare(other)
    other
  end

  private

  # Callable only without an explicit receiver.
  def recompute
    @balance
  end

  public

  # Back to public again.
  def balance
    @balance
  end

  # Applies to this single method only, without flipping the default.
  private def audit
    @balance
  end

  # A trailing public method to prove the `private def` above did not flip the default.
  def close
    @balance = 0
  end

  # Defined public above, then retroactively re-tagged private via the multi-symbol form.
  def withdraw
    @balance
  end

  def transfer
    @balance
  end

  private :withdraw, :transfer
end

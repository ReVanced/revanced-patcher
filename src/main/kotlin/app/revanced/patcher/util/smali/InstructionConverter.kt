package app.revanced.patcher.util.smali

import org.jf.dexlib2.Format
import org.jf.dexlib2.builder.instruction.*
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.formats.*
import org.jf.util.ExceptionWithContext

fun Instruction.toBuilderInstruction() =
    when (this.opcode.format) {
        Format.Format10x -> InstructionConverter.newBuilderInstruction10x(this as Instruction10x)
        Format.Format11n -> InstructionConverter.newBuilderInstruction11n(this as Instruction11n)
        Format.Format11x -> InstructionConverter.newBuilderInstruction11x(this as Instruction11x)
        Format.Format12x -> InstructionConverter.newBuilderInstruction12x(this as Instruction12x)
        Format.Format20bc -> InstructionConverter.newBuilderInstruction20bc(this as Instruction20bc)
        Format.Format21c -> InstructionConverter.newBuilderInstruction21c(this as Instruction21c)
        Format.Format21ih -> InstructionConverter.newBuilderInstruction21ih(this as Instruction21ih)
        Format.Format21lh -> InstructionConverter.newBuilderInstruction21lh(this as Instruction21lh)
        Format.Format21s -> InstructionConverter.newBuilderInstruction21s(this as Instruction21s)
        Format.Format22b -> InstructionConverter.newBuilderInstruction22b(this as Instruction22b)
        Format.Format22c -> InstructionConverter.newBuilderInstruction22c(this as Instruction22c)
        Format.Format22cs -> InstructionConverter.newBuilderInstruction22cs(this as Instruction22cs)
        Format.Format22s -> InstructionConverter.newBuilderInstruction22s(this as Instruction22s)
        Format.Format22x -> InstructionConverter.newBuilderInstruction22x(this as Instruction22x)
        Format.Format23x -> InstructionConverter.newBuilderInstruction23x(this as Instruction23x)
        Format.Format31c -> InstructionConverter.newBuilderInstruction31c(this as Instruction31c)
        Format.Format31i -> InstructionConverter.newBuilderInstruction31i(this as Instruction31i)
        Format.Format32x -> InstructionConverter.newBuilderInstruction32x(this as Instruction32x)
        Format.Format35c -> InstructionConverter.newBuilderInstruction35c(this as Instruction35c)
        Format.Format35mi -> InstructionConverter.newBuilderInstruction35mi(this as Instruction35mi)
        Format.Format35ms -> InstructionConverter.newBuilderInstruction35ms(this as Instruction35ms)
        Format.Format3rc -> InstructionConverter.newBuilderInstruction3rc(this as Instruction3rc)
        Format.Format3rmi -> InstructionConverter.newBuilderInstruction3rmi(this as Instruction3rmi)
        Format.Format3rms -> InstructionConverter.newBuilderInstruction3rms(this as Instruction3rms)
        Format.Format51l -> InstructionConverter.newBuilderInstruction51l(this as Instruction51l)
        else -> throw ExceptionWithContext("Instruction format %s not supported", this.opcode.format)
    }

internal class InstructionConverter {
    companion object {
        internal fun newBuilderInstruction10x(instruction: Instruction10x): BuilderInstruction10x {
            return BuilderInstruction10x(
                instruction.opcode
            )
        }

        internal fun newBuilderInstruction11n(instruction: Instruction11n): BuilderInstruction11n {
            return BuilderInstruction11n(
                instruction.opcode,
                instruction.registerA,
                instruction.narrowLiteral
            )
        }

        internal fun newBuilderInstruction11x(instruction: Instruction11x): BuilderInstruction11x {
            return BuilderInstruction11x(
                instruction.opcode,
                instruction.registerA
            )
        }

        internal fun newBuilderInstruction12x(instruction: Instruction12x): BuilderInstruction12x {
            return BuilderInstruction12x(
                instruction.opcode,
                instruction.registerA,
                instruction.registerB
            )
        }

        internal fun newBuilderInstruction20bc(instruction: Instruction20bc): BuilderInstruction20bc {
            return BuilderInstruction20bc(
                instruction.opcode,
                instruction.verificationError,
                instruction.reference
            )
        }

        internal fun newBuilderInstruction21c(instruction: Instruction21c): BuilderInstruction21c {
            return BuilderInstruction21c(
                instruction.opcode,
                instruction.registerA,
                instruction.reference
            )
        }

        internal fun newBuilderInstruction21ih(instruction: Instruction21ih): BuilderInstruction21ih {
            return BuilderInstruction21ih(
                instruction.opcode,
                instruction.registerA,
                instruction.narrowLiteral
            )
        }

        internal fun newBuilderInstruction21lh(instruction: Instruction21lh): BuilderInstruction21lh {
            return BuilderInstruction21lh(
                instruction.opcode,
                instruction.registerA,
                instruction.wideLiteral
            )
        }

        internal fun newBuilderInstruction21s(instruction: Instruction21s): BuilderInstruction21s {
            return BuilderInstruction21s(
                instruction.opcode,
                instruction.registerA,
                instruction.narrowLiteral
            )
        }

        internal fun newBuilderInstruction22b(instruction: Instruction22b): BuilderInstruction22b {
            return BuilderInstruction22b(
                instruction.opcode,
                instruction.registerA,
                instruction.registerB,
                instruction.narrowLiteral
            )
        }

        internal fun newBuilderInstruction22c(instruction: Instruction22c): BuilderInstruction22c {
            return BuilderInstruction22c(
                instruction.opcode,
                instruction.registerA,
                instruction.registerB,
                instruction.reference
            )
        }

        internal fun newBuilderInstruction22cs(instruction: Instruction22cs): BuilderInstruction22cs {
            return BuilderInstruction22cs(
                instruction.opcode,
                instruction.registerA,
                instruction.registerB,
                instruction.fieldOffset
            )
        }

        internal fun newBuilderInstruction22s(instruction: Instruction22s): BuilderInstruction22s {
            return BuilderInstruction22s(
                instruction.opcode,
                instruction.registerA,
                instruction.registerB,
                instruction.narrowLiteral
            )
        }

        internal fun newBuilderInstruction22x(instruction: Instruction22x): BuilderInstruction22x {
            return BuilderInstruction22x(
                instruction.opcode,
                instruction.registerA,
                instruction.registerB
            )
        }

        internal fun newBuilderInstruction23x(instruction: Instruction23x): BuilderInstruction23x {
            return BuilderInstruction23x(
                instruction.opcode,
                instruction.registerA,
                instruction.registerB,
                instruction.registerC
            )
        }

        internal fun newBuilderInstruction31c(instruction: Instruction31c): BuilderInstruction31c {
            return BuilderInstruction31c(
                instruction.opcode,
                instruction.registerA,
                instruction.reference
            )
        }

        internal fun newBuilderInstruction31i(instruction: Instruction31i): BuilderInstruction31i {
            return BuilderInstruction31i(
                instruction.opcode,
                instruction.registerA,
                instruction.narrowLiteral
            )
        }

        internal fun newBuilderInstruction32x(instruction: Instruction32x): BuilderInstruction32x {
            return BuilderInstruction32x(
                instruction.opcode,
                instruction.registerA,
                instruction.registerB
            )
        }

        internal fun newBuilderInstruction35c(instruction: Instruction35c): BuilderInstruction35c {
            return BuilderInstruction35c(
                instruction.opcode,
                instruction.registerCount,
                instruction.registerC,
                instruction.registerD,
                instruction.registerE,
                instruction.registerF,
                instruction.registerG,
                instruction.reference
            )
        }

        internal fun newBuilderInstruction35mi(instruction: Instruction35mi): BuilderInstruction35mi {
            return BuilderInstruction35mi(
                instruction.opcode,
                instruction.registerCount,
                instruction.registerC,
                instruction.registerD,
                instruction.registerE,
                instruction.registerF,
                instruction.registerG,
                instruction.inlineIndex
            )
        }

        internal fun newBuilderInstruction35ms(instruction: Instruction35ms): BuilderInstruction35ms {
            return BuilderInstruction35ms(
                instruction.opcode,
                instruction.registerCount,
                instruction.registerC,
                instruction.registerD,
                instruction.registerE,
                instruction.registerF,
                instruction.registerG,
                instruction.vtableIndex
            )
        }

        internal fun newBuilderInstruction3rc(instruction: Instruction3rc): BuilderInstruction3rc {
            return BuilderInstruction3rc(
                instruction.opcode,
                instruction.startRegister,
                instruction.registerCount,
                instruction.reference
            )
        }

        internal fun newBuilderInstruction3rmi(instruction: Instruction3rmi): BuilderInstruction3rmi {
            return BuilderInstruction3rmi(
                instruction.opcode,
                instruction.startRegister,
                instruction.registerCount,
                instruction.inlineIndex
            )
        }

        internal fun newBuilderInstruction3rms(instruction: Instruction3rms): BuilderInstruction3rms {
            return BuilderInstruction3rms(
                instruction.opcode,
                instruction.startRegister,
                instruction.registerCount,
                instruction.vtableIndex
            )
        }

        internal fun newBuilderInstruction51l(instruction: Instruction51l): BuilderInstruction51l {
            return BuilderInstruction51l(
                instruction.opcode,
                instruction.registerA,
                instruction.wideLiteral
            )
        }
    }
}